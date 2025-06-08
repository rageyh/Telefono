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

    private ServiceManager serviceManager;
    private boolean eventsRegistered = false;
    @Getter
    private static Telefono instance;

    @Deprecated
    public static ServiceManager getServiceManager() {
        return instance != null ? instance.serviceManager : null;
    }
    
    @Deprecated
    public static CacheAbbonamento getCacheAbbonamento() {
        return instance != null ? instance.serviceManager.getCacheAbbonamento() : null;
    }
    
    @Deprecated  
    public static CacheContatti getCacheContatti() {
        return instance != null ? instance.serviceManager.getCacheContatti() : null;
    }
    
    @Deprecated
    public static CacheNumeri getCacheNumeri() {
        return instance != null ? instance.serviceManager.getCacheNumeri() : null;
    }
    
    @Deprecated
    public static CacheChiamata getCacheChiamata() {
        return instance != null ? instance.serviceManager.getCacheChiamata() : null;
    }
    
    @Deprecated
    public static CacheHistoryChiamate getCacheHistoryChiamate() {
        return instance != null ? instance.serviceManager.getCacheHistoryChiamate() : null;
    }
    
    @Deprecated
    public static CacheHistoryMessaggi getCacheHistoryMessaggi() {
        return instance != null ? instance.serviceManager.getCacheHistoryMessaggi() : null;
    }
    
    @Deprecated
    public static HeadDatabaseAPI getHeadDatabaseAPI() {
        return instance != null ? instance.serviceManager.getHeadDatabaseAPI() : null;
    }

    @Override
    protected void onReloadablesStart() {
        if (serviceManager != null) {
            Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::forceRestoreInventory);
            serviceManager.shutdown();
        }
        
        if (eventsRegistered) {
            unregisterEvents();
        }
        
        initServiceManager();
        
        Valid.checkBoolean(Common.doesPluginExist("ItemsAdder"), "ItemsAdder non trovato, controlla che sia inserito come plugin.");
        Valid.checkBoolean(Common.doesPluginExist("HeadDatabase"), "HeadDatabase non trovato, controlla che sia inserito come plugin.");
    }

    @Override
    protected void onPluginPreReload() {
        if (serviceManager != null) {
            Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::forceRestoreInventory);
            serviceManager.shutdown();
        }
    }

    @Override
    protected void onPluginStart() {
        instance = this;
        
        printStartupHeader();
        initServiceManager();
        
        serviceManager.initialize().thenRun(() -> {
            printStartupSuccess();
        }).exceptionally(throwable -> {
            printStartupError(throwable);
            return null;
        });
    }

    private void initServiceManager() {
        serviceManager = new ServiceManager();

        if (!eventsRegistered) {
            registerEvents(
                new EventInteractMainMenu(), 
                new EventOpenTelephone(), 
                new EventUseSim(), 
                new PlayerCleanupListener(), 
                new CallMonitoringListener(serviceManager), 
                new CacheInvalidationListener()
            );
            eventsRegistered = true;
        }
    }

    @Override
    protected void onPluginStop() {
        printShutdownHeader();
        
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::forceRestoreInventory);
        
        if (serviceManager != null) {
            serviceManager.shutdown();
        }
        
        instance = null;
        printShutdownComplete();
    }
    
    private void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }
    
    private void unregisterEvents() {
        try {
            org.bukkit.event.HandlerList.unregisterAll((org.bukkit.plugin.Plugin) this);
            eventsRegistered = false;
        } catch (Exception e) {
            Common.error(e, "Errore durante de-registrazione eventi");
        }
    }
    
    private void printStartupHeader() {
        Common.log(" ");
        Common.log("&8┌─────────────────────────────────────────────────────────────┐");
        Common.log("&8│                                                             │");
        Common.log("&8│      &9&l⚡ TELEFONO SYSTEM &8│ &7Version &f" + getDescription().getVersion() + "                 &8│");
        Common.log("&8│      &7Advanced Telephone Plugin for Minecraft             &8│");
        Common.log("&8│                                                             │");
        Common.log("&8│      &7Status: &e⏳ Initializing...                         &8│");
        Common.log("&8│                                                             │");
        Common.log("&8└─────────────────────────────────────────────────────────────┘");
        Common.log(" ");
    }
    
    private void printStartupSuccess() {
        Common.log(" ");
        Common.log("&8┌─────────────────────────────────────────────────────────────┐");
        Common.log("&8│                                                             │");
        Common.log("&8│      &a&l✓ TELEFONO SYSTEM READY &8│ &7Performance Mode      &8│");
        Common.log("&8│      &7Multi-level Caching: &a&lENABLED                     &8│");
        Common.log("&8│      &7Security Validation: &a&lACTIVE                      &8│");
        Common.log("&8│      &7Database Pool: &a&lOPTIMIZED                         &8│");
        Common.log("&8│                                                             │");
        Common.log("&8│      &7Status: &a&l🚀 ONLINE &8│ &7Ready for players        &8│");
        Common.log("&8│                                                             │");
        Common.log("&8└─────────────────────────────────────────────────────────────┘");
        Common.log(" ");
    }
    
    private void printStartupError(Throwable throwable) {
        Common.log(" ");
        Common.log("&8┌─────────────────────────────────────────────────────────────┐");
        Common.log("&8│                                                             │");
        Common.log("&8│      &c&l✗ TELEFONO SYSTEM FAILED &8│ &7Startup Error       &8│");
        Common.log("&8│      &7System could not initialize properly                 &8│");
        Common.log("&8│                                                             │");
        Common.log("&8│      &7Status: &c&l⚠ ERROR &8│ &7Check logs below          &8│");
        Common.log("&8│                                                             │");
        Common.log("&8└─────────────────────────────────────────────────────────────┘");
        Common.log(" ");
        Common.error(throwable, "Dettagli errore inizializzazione");
    }
    
    private void printShutdownHeader() {
        Common.log(" ");
        Common.log("&8┌─────────────────────────────────────────────────────────────┐");
        Common.log("&8│      &e&l⏸ TELEFONO SYSTEM SHUTDOWN &8│ &7Graceful Stop     &8│");
        Common.log("&8│      &7Saving data and cleaning resources...               &8│");
        Common.log("&8└─────────────────────────────────────────────────────────────┘");
        Common.log(" ");
    }
    
    private void printShutdownComplete() {
        Common.log(" ");
        Common.log("&8┌─────────────────────────────────────────────────────────────┐");
        Common.log("&8│      &7&l💤 TELEFONO SYSTEM OFFLINE &8│ &7Clean Shutdown    &8│");
        Common.log("&8│      &7All resources cleaned successfully                   &8│");
        Common.log("&8└─────────────────────────────────────────────────────────────┘");
        Common.log(" ");
    }
}
