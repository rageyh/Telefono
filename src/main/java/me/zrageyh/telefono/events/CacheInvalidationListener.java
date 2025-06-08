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
} 