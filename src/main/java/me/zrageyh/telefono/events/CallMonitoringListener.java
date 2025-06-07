package me.zrageyh.telefono.events;

import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Call;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;

import java.util.Optional;

/*
 * Sostituisce il polling con event-driven call monitoring
 * Riduce drasticamente l'overhead CPU per 200+ player
 */
public class CallMonitoringListener implements Listener {

    private final ServiceManager serviceManager;

    public CallMonitoringListener(final ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    /* Gestisce disconnessione durante chiamata */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        final String simNumber = getTelephoneNumber(p);
        if (simNumber == null) return;
        final Optional<Call> activeCall = serviceManager.getCacheChiamata().getData(simNumber);

        activeCall.ifPresent(call -> endCallSafely(call, p, "disconnessione"));
    }

    /* Monitora cambio item in mano */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(final PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        final ItemStack newItem = p.getInventory().getItem(e.getNewSlot());

        // Se non ha più telefono in mano
        if (!isTelephoneItem(newItem) && !isTelephoneItem(p.getInventory().getItemInOffHand())) {
            checkAndEndCall(p, "telefono non in mano");
        }
    }

    /* Monitora drop del telefono */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop(final PlayerDropItemEvent e) {
        final Player p = e.getPlayer();
        final ItemStack dropped = e.getItemDrop().getItemStack();

        if (isTelephoneItem(dropped)) {
            // Verifica se ha ancora un telefono
            if (!isTelephoneItem(p.getInventory().getItemInMainHand()) &&
                !isTelephoneItem(p.getInventory().getItemInOffHand())) {
                checkAndEndCall(p, "telefono non presente nella mano");
            }
        }
    }

    /* Monitora swap tra main/off hand */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSwap(final PlayerSwapHandItemsEvent e) {
        final Player p = e.getPlayer();

        // Schedula controllo dopo lo swap
        Common.runLater(1, () -> {
            if (!isTelephoneItem(p.getInventory().getItemInMainHand()) &&
                !isTelephoneItem(p.getInventory().getItemInOffHand())) {
                checkAndEndCall(p, "telefono non disponibile");
            }
        });
    }

    /* Verifica e termina chiamata se necessario */
    private void checkAndEndCall(final Player p, final String reason) {
        final String simNumber = getTelephoneNumber(p);
        if (simNumber == null) return;

        final Optional<Call> activeCall = serviceManager.getCacheChiamata().getData(simNumber);
        if (activeCall.isPresent()) {
            endCallSafely(activeCall.get(), p, reason);
        }
    }

    /* Termina chiamata in modo sicuro */
    private void endCallSafely(final Call call, final Player trigger, final String reason) {
        try {
            // Ottieni entrambi i giocatori
            final Player caller = call.getContattoWhoCall().getPlayer();
            final Player receiver = call.getContattoCalled().getPlayer();

            // Termina la chiamata
            serviceManager.getCacheChiamata().removeData(call.getContattoWhoCall().getSim());

            // Salva cronologia in modo asincrono
            serviceManager.getDatabase().saveChiamata(call.getHistoryChiamata(false))
                .thenRun(() -> {
                    // Aggiorna cache cronologia
                    serviceManager.getCacheHistoryChiamate().put(
                        call.getContattoCalled().getSim(),
                        call.getHistoryChiamata(false)
                    );
                    serviceManager.getCacheHistoryChiamate().put(
                        call.getContattoCalled().getNumber(),
                        call.getHistoryChiamataReverse(false)
                    );
                })
                .exceptionally(throwable -> {
                    Common.error(throwable, "Errore salvando cronologia chiamata terminata per: " + reason);
                    return null;
                });

            // Notifica giocatori
            final String message = "&7✉ &f&lCHIAMATA &8» &cChiamata terminata (" + reason + ")";

            if (caller != null && caller.isOnline()) {
                Common.tellNoPrefix(caller, message);
            }

            if (receiver != null && receiver.isOnline()) {
                Common.tellNoPrefix(receiver, message);
            }

            Common.log("Chiamata terminata automaticamente: " + reason + " (Player: " + trigger.getName() + ")");

        } catch (final Exception e) {
            Common.error(e, "Errore terminando chiamata per: " + reason);
        }
    }

    /* Ottiene numero SIM dal telefono del giocatore */
    private String getTelephoneNumber(final Player p) {
        final ItemStack mainHand = p.getInventory().getItemInMainHand();
        final ItemStack offHand = p.getInventory().getItemInOffHand();

        if (isTelephoneItem(mainHand)) {
            return TelephoneAPI.getTelephoneNumber(mainHand);
        }

        if (isTelephoneItem(offHand)) {
            return TelephoneAPI.getTelephoneNumber(offHand);
        }

        return null;
    }

    /* Verifica se l'item è un telefono */
    private boolean isTelephoneItem(final ItemStack item) {
        return item != null && TelephoneAPI.isTelephone(item);
    }
} 