package me.zrageyh.telefono.events;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.model.Contatto;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.conversation.SimpleConversation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class StartCall extends SimpleConversation {

    private final Contatto contattoCalled;
    private final Contatto contattoWhoCall;
    private final Player player;
    private final Player target;
    private final String fullNameWhoCall;
    private final String fullNameCalled;
    private final Abbonamento abbonamento;
    private final Call call;

    // Thread safety e task management
    private BukkitTask billingTask;
    private BukkitTask monitoringTask;
    private final AtomicBoolean callEnded = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    public StartCall(final Player player, final Call call) {
        this.call = call;
        contattoCalled = call.getContattoCalled();
        contattoWhoCall = call.getContattoWhoCall();
        this.player = player;
        target = contattoCalled.getPlayer();
        abbonamento = call.getAbbonamento();
        fullNameWhoCall = contattoWhoCall.getFullName();
        fullNameCalled = contattoCalled.getFullName();
        start();
    }

    private void start() {
        // Solo task per billing - il monitoring è gestito da CallMonitoringListener
        billingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (callEnded.get()) {
                    cancel();
                    return;
                }

                if (!Telefono.getCacheChiamata().containsNumber(contattoWhoCall.getSim())) {
                    endCallSafely();
                    cancel();
                    return;
                }

                if (contattoCalled.getPlayer().equals(player)) {
                    endCallSafely();
                    cancel();
                    return;
                }

                // Thread-safe update dell'abbonamento tramite cache
                updateAbbonamentoSafely();
            }
        }.runTaskTimerAsynchronously(Telefono.getInstance(), 0, 20L * 60);

        // Il monitoring delle disconnessioni e drop telefono è gestito da CallMonitoringListener
        // Rimuovo il polling task per ridurre overhead CPU
    }

    /* Thread-safe update dell'abbonamento */
    private void updateAbbonamentoSafely() {
        lock.lock();
        try {
            if (callEnded.get()) return;

            Telefono.getCacheAbbonamento().get(abbonamento.getSim())
                .thenAccept(optAbb -> {
                    if (optAbb.isPresent() && !callEnded.get()) {
                        final Abbonamento current = optAbb.get();
                        if (current.hasCreditoToCall()) {
                            current.removeMinute();
                            Telefono.getCacheAbbonamento().update(current);

                            // Salva in modo asincrono
                            Database.getInstance().updateSubscription(current)
                                .exceptionally(throwable -> {
                                    Common.error(throwable, "Errore aggiornamento abbonamento durante chiamata");
                                    return null;
                                });
                        } else {
                            // Termina chiamata se credito finito
                            Common.runLater(() -> endCallSafely());
                        }
                    }
                })
                .exceptionally(throwable -> {
                    Common.error(throwable, "Errore recupero abbonamento durante chiamata");
                    return null;
                });
        } finally {
            lock.unlock();
        }
    }

    /* Thread-safe end call con cleanup completo */
    private void endCallSafely() {
        if (!callEnded.compareAndSet(false, true)) {
            return; // Già terminata
        }

        lock.lock();
        try {
            // Cleanup dei task
            cleanup();

            // Salva cronologia in modo asincrono
            Database.getInstance().saveChiamata(call.getHistoryChiamata(false))
                .thenRun(() -> {
                    Telefono.getCacheHistoryChiamate().put(contattoCalled.getSim(), call.getHistoryChiamata(false));
                    Telefono.getCacheHistoryChiamate().put(contattoCalled.getNumber(), call.getHistoryChiamataReverse(false));
                })
                .exceptionally(throwable -> {
                    Common.error(throwable, "Errore salvataggio cronologia chiamata");
                    return null;
                });

            // Aggiorna abbonamento finale
            Telefono.getCacheAbbonamento().update(abbonamento);

            // Rimuovi dalla cache chiamate attive
            Telefono.getCacheChiamata().removeData(contattoCalled.getSim());

            // Notifica giocatori
            if (player != null && player.isOnline()) {
                Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &cLa chiamata con %s è terminata ed è stata salvata nel registro chiamate".formatted(contattoCalled.getFullName()));
            }
            if (target != null && target.isOnline()) {
                Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &cLa chiamata con %s è terminata ed è stata salvata nel registro chiamate".formatted(contattoWhoCall.getFullName()));
            }
        } finally {
            lock.unlock();
        }
    }

    /* Cleanup dei task per evitare memory leak */
    public void cleanup() {
        if (billingTask != null && !billingTask.isCancelled()) {
            billingTask.cancel();
        }
        // monitoringTask rimosso - gestito da CallMonitoringListener
    }

    @Override
    protected Prompt getFirstPrompt() {
        return new Prompt() {
            @Override
            public @NotNull String getPromptText(@NotNull final ConversationContext context) {
                if (player.equals(contattoWhoCall.getPlayer())) {
                    return "§a%s ha accettato la tua chiamata".formatted(fullNameCalled);
                } else {
                    return "§aHai accettato la chiamata di %s".formatted(fullNameWhoCall);
                }
            }

            @Override
            public boolean blocksForInput(@NotNull final ConversationContext context) {
                return true;
            }

            @Override
            public @Nullable Prompt acceptInput(@NotNull final ConversationContext context, @Nullable final String input) {
                final Player player = (Player) context.getForWhom();

                if (callEnded.get() || !Telefono.getCacheChiamata().containsNumber(contattoWhoCall.getSim())) {
                    player.chat(input);
                    return Prompt.END_OF_CONVERSATION;
                }

                if (player.equals(contattoWhoCall.getPlayer())) {
                    final Player target = contattoCalled.getPlayer();
                    Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &fTu: &7%s".formatted(input));
                    if (target != null && target.isOnline()) {
                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &f%s: &7%s".formatted(fullNameWhoCall, input));
                    }
                } else {
                    final Player target = contattoWhoCall.getPlayer();
                    Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &fTu: &7%s".formatted(input));
                    if (target != null && target.isOnline()) {
                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &f%s: &7%s".formatted(fullNameCalled, input));
                    }
                }
                return this;
            }
        };
    }
}
