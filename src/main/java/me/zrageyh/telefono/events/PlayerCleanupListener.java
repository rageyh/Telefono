package me.zrageyh.telefono.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/* Cleanup automatico delle cache per prevenire memory leak */
public class PlayerCleanupListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        EventInteractMainMenu.restoreInventory(p);
    }
} 