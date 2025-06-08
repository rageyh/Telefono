package me.zrageyh.telefono.manager;

import lombok.Getter;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Call;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager centralizzato per gestire il billing di tutte le chiamate attive
 * Risolve il problema di performance dei task multipli usando un singolo timer
 */
public final class CallBillingManager {

    @Getter
    private static final CallBillingManager instance = new CallBillingManager();

    // Thread-safe tracking delle chiamate attive
    private final Map<String, CallBillingInfo> activeCallBillings = new ConcurrentHashMap<>();
    private BukkitTask centralBillingTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Performance: Billing ogni minuto per tutte le chiamate
    private static final long BILLING_INTERVAL_TICKS = 20L * 60; // 1 minuto

    private CallBillingManager() {
        // Singleton
    }

    /**
     * Registra una chiamata per il billing centralizzato
     */
    public void registerCallForBilling(final String simNumber, final Call call, final Runnable onCreditExpired) {
        final CallBillingInfo billingInfo = new CallBillingInfo(call, onCreditExpired, System.currentTimeMillis());
        activeCallBillings.put(simNumber, billingInfo);

        // Avvia il task centralizzato se non è già in esecuzione
        startCentralBillingIfNeeded();

        Common.log("Registered call billing for SIM: " + simNumber + " (Total active: " + activeCallBillings.size() + ")");
    }

    /**
     * Rimuove una chiamata dal billing
     */
    public void unregisterCallBilling(final String simNumber) {
        final CallBillingInfo removed = activeCallBillings.remove(simNumber);
        if (removed != null) {
            Common.log("Unregistered call billing for SIM: " + simNumber + " (Remaining active: " + activeCallBillings.size() + ")");
        }

        // Ferma il task se non ci sono più chiamate attive
        stopCentralBillingIfEmpty();
    }

    /**
     * Avvia il task centralizzato solo se necessario (lazy initialization)
     */
    private void startCentralBillingIfNeeded() {
        if (isRunning.compareAndSet(false, true)) {
                         centralBillingTask = Common.runTimerAsync((int) BILLING_INTERVAL_TICKS, (int) BILLING_INTERVAL_TICKS, this::processBillingForAllCalls);
            Common.log("Started central call billing task");
        }
    }

    /**
     * Ferma il task centralizzato se non ci sono chiamate attive
     */
    private void stopCentralBillingIfEmpty() {
        if (activeCallBillings.isEmpty() && isRunning.compareAndSet(true, false)) {
            if (centralBillingTask != null && !centralBillingTask.isCancelled()) {
                centralBillingTask.cancel();
                centralBillingTask = null;
            }
            Common.log("Stopped central call billing task - no active calls");
        }
    }

    /**
     * Processa il billing per tutte le chiamate attive in un singolo task
     */
    private void processBillingForAllCalls() {
        if (activeCallBillings.isEmpty()) {
            stopCentralBillingIfEmpty();
            return;
        }

        // Processa tutte le chiamate attive
        activeCallBillings.entrySet().removeIf(entry -> {
            final String simNumber = entry.getKey();
            final CallBillingInfo billingInfo = entry.getValue();

            // Verifica se la chiamata è ancora attiva
            if (!Telefono.getCacheChiamata().containsNumber(simNumber)) {
                Common.log("Call billing stopped for SIM " + simNumber + " - call no longer active");
                return true; // Rimuovi dal billing
            }

            // Processa il billing per questa chiamata
            return processSingleCallBilling(simNumber, billingInfo);
        });

        Common.log("Processed billing for " + activeCallBillings.size() + " active calls");
    }

    /**
     * Processa il billing per una singola chiamata
     */
    private boolean processSingleCallBilling(final String simNumber, final CallBillingInfo billingInfo) {
        try {
            Telefono.getCacheAbbonamento().get(simNumber)
                .thenAccept(optAbb -> {
                    if (optAbb.isPresent()) {
                        final Abbonamento current = optAbb.get();
                        if (current.hasCreditoToCall()) {
                            // Decrementa credito
                            current.removeMinute();
                            Telefono.getCacheAbbonamento().update(current);

                            // Salva in modo asincrono
                            Database.getInstance().updateSubscription(current)
                                .exceptionally(throwable -> {
                                    Common.error(throwable, "Errore aggiornamento abbonamento durante billing centralizzato: " + simNumber);
                                    return null;
                                });

                            // Update billing info
                            billingInfo.updateLastBilled();

                        } else {
                            // Credito esaurito - termina chiamata
                            Common.runLater(() -> {
                                if (billingInfo.onCreditExpired != null) {
                                    billingInfo.onCreditExpired.run();
                                }
                            });
                            // Rimuovi dal billing
                            unregisterCallBilling(simNumber);
                        }
                    } else {
                        // Abbonamento non trovato - termina billing
                        Common.warning("Abbonamento non trovato per SIM durante billing: " + simNumber);
                        unregisterCallBilling(simNumber);
                    }
                })
                .exceptionally(throwable -> {
                    Common.error(throwable, "Errore recupero abbonamento durante billing centralizzato: " + simNumber);
                    return null;
                });

            return false; // Non rimuovere dal billing

        } catch (final Exception e) {
            Common.error(e, "Errore durante billing per SIM: " + simNumber);
            return true; // Rimuovi dal billing per sicurezza
        }
    }

    /**
     * Shutdown del manager per cleanup
     */
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            if (centralBillingTask != null && !centralBillingTask.isCancelled()) {
                centralBillingTask.cancel();
            }
        }
        activeCallBillings.clear();
        Common.log("CallBillingManager shutdown completed");
    }

    /**
     * Statistiche per monitoring
     */
    public int getActiveCallsCount() {
        return activeCallBillings.size();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Classe interna per tracking delle info di billing
     */
    private static final class CallBillingInfo {
        final Call call;
        final Runnable onCreditExpired;
        private long lastBilledTimestamp;

        CallBillingInfo(final Call call, final Runnable onCreditExpired, final long startTimestamp) {
            this.call = call;
            this.onCreditExpired = onCreditExpired;
            lastBilledTimestamp = startTimestamp;
        }

        void updateLastBilled() {
            lastBilledTimestamp = System.currentTimeMillis();
        }

        long getLastBilledTimestamp() {
            return lastBilledTimestamp;
        }
    }
} 