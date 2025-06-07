package me.zrageyh.telefono;


import lombok.Getter;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.cache.*;
import me.zrageyh.telefono.events.*;
import me.zrageyh.telefono.items.paginated.BackItem;
import me.zrageyh.telefono.items.paginated.ForwardItem;
import me.zrageyh.telefono.manager.ServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.gui.structure.Structure;

import java.util.Arrays;

@Getter
public final class Telefono extends SimplePlugin {

    
    @Getter
    private static ServiceManager serviceManager;
    
    // Backward compatibility getters
    @Getter
    private static CacheAbbonamento cacheAbbonamento;
    @Getter
    private static CacheContatti cacheContatti;
    @Getter
    private static CacheNumeri cacheNumeri;
    @Getter
    private static HeadDatabaseAPI headDatabaseAPI;
    @Getter
    private static CacheChiamata cacheChiamata;
    @Getter
    private static CacheHistoryChiamate cacheHistoryChiamate;
    @Getter
    private static CacheHistoryMessaggi cacheHistoryMessaggi;
    // telephoneService rimosso - logica migrata in listeners
    // rateLimitService rimosso - non utilizzato

    @Override
    protected void onReloadablesStart() {
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        
        if (serviceManager != null) {
            serviceManager.shutdown();
        }
        
        initServiceManager();
        Valid.checkBoolean(Common.doesPluginExist("ItemsAdder"), "ItemsAdder non trovato, controlla che sia inserito come plugin.");
        Valid.checkBoolean(Common.doesPluginExist("HeadDatabase"), "HeadDatabase non trovato, controlla che sia inserito come plugin.");
    }

    @Override
    protected void onPluginPreReload() {
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        
        if (serviceManager != null) {
            serviceManager.shutdown();
        }
    }

    @Override
    protected void onPluginStart() {
        Common.log("&8&m-----------------------------------------------------");
        Common.log("&9&lTelefono v" + getDescription().getVersion() + " &7by &9zRageyh_");
        Common.log("&7Avvio in corso...");
        Common.log("&8&m-----------------------------------------------------");

        // Initialize services
        serviceManager.initialize().thenRun(() -> {
            Common.log("&8&m-----------------------------------------------------");
            Common.log("&a&l✓ Plugin avviato con successo!");
            Common.log("&8&m-----------------------------------------------------");
        }).exceptionally(throwable -> {
            Common.log("&8&m-----------------------------------------------------");
            Common.error(throwable, "&c&l✗ Errore durante l'avvio del plugin");
            Common.log("&8&m-----------------------------------------------------");
            return null;
        });
    }

    /* Inizializza ServiceManager e imposta cache statiche per backward compatibility */
    private void initServiceManager() {
        serviceManager = new ServiceManager();

        registerEvents(new EventInteractMainMenu(), new EventOpenTelephone(), new EventUseSim(), new PlayerCleanupListener(), new CallMonitoringListener(serviceManager), new CacheInvalidationListener());
        serviceManager.initialize().thenRun(() -> {
            cacheAbbonamento = serviceManager.getCacheAbbonamento();
            cacheContatti = serviceManager.getCacheContatti();
            cacheNumeri = serviceManager.getCacheNumeri();
            cacheChiamata = serviceManager.getCacheChiamata();
            cacheHistoryChiamate = serviceManager.getCacheHistoryChiamate();
            cacheHistoryMessaggi = serviceManager.getCacheHistoryMessaggi();
            headDatabaseAPI = serviceManager.getHeadDatabaseAPI();
            
            Common.log("ServiceManager inizializzato correttamente");
        }).exceptionally(throwable -> {
            Common.error(throwable, "Errore durante inizializzazione ServiceManager");
            return null;
        });
    }

    @Override
    protected void onPluginStop() {
        Common.log("Iniziando shutdown del plugin Telefono...");
        
        // Ripristina inventari giocatori
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        
        // Shutdown ServiceManager (gestisce tutto il cleanup)
        if (serviceManager != null) {
            serviceManager.shutdown();
        }
        
        Common.log("Shutdown del plugin Telefono completato");
    }
    private void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

}
