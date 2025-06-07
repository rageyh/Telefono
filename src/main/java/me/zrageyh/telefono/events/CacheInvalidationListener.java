package me.zrageyh.telefono.events;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;

import java.util.List;

/*
 * Listener per invalidazione intelligente delle cache
 * Ottimizza le performance invalidando solo le cache necessarie
 */
public class CacheInvalidationListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final String sim = getPlayerSim(event.getPlayer());
        if (sim != null) {
            // Pre-load cache per il giocatore che entra
            Common.runAsync(() -> {
                Telefono.getCacheContatti().get(sim);
                Telefono.getCacheAbbonamento().get(sim);
                Common.log("Cache pre-loaded per giocatore: " + event.getPlayer().getName() + " (SIM: " + sim + ")");
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final String sim = getPlayerSim(event.getPlayer());
        if (sim != null) {
            Common.runLaterAsync(60 * 20, () -> {
                Telefono.getCacheContatti().getCache().invalidate(sim);
                Common.runLaterAsync(300 * 20, () -> {
                    Telefono.getCacheAbbonamento().getCache().invalidate(sim);
                });
            });
        }
    }

    /* Ottieni numero SIM del giocatore */
    private String getPlayerSim(final Player player) {
        if (!TelephoneAPI.hasTelephoneInInventory(player)) {
            return null;
        }

        final List<ItemStack> telephones = TelephoneAPI.getTelephonesInInventory(player);
        if (!telephones.isEmpty()) {
            return TelephoneAPI.getTelephoneNumber(telephones.getFirst());
        }
        return null;
    }

    /* Invalida cache su eventi di modifica dati */
    public static void invalidateOnContactChange(final String sim) {
        Common.runAsync(() -> {
            Telefono.getCacheContatti().getCache().invalidate(sim);
            Common.log("Cache contatti invalidata per SIM: " + sim);
        });
    }

    public static void invalidateOnSubscriptionChange(final String sim) {
        Common.runAsync(() -> {
            Telefono.getCacheAbbonamento().getCache().invalidate(sim);
            Common.log("Cache abbonamento invalidata per SIM: " + sim);
        });
    }

    /* Invalidazione batch per operazioni massive */
    public static void invalidateAllCaches() {
        Common.runAsync(() -> {
            Telefono.getCacheContatti().getCache().invalidateAll();
            Telefono.getCacheAbbonamento().getCache().invalidateAll();
            // Note: CacheHistoryChiamate e CacheHistoryMessaggi non hanno invalidateAll()
            // implementeremo solo per le cache principali
            Common.log("Cache principali invalidate");
        });
    }
} 