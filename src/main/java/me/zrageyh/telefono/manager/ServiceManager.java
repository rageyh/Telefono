package me.zrageyh.telefono.manager;

import lombok.Getter;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.cache.*;
// RateLimitService rimosso - non utilizzato
// TelephoneService rimosso - logica migrata in event listeners
import me.zrageyh.telefono.items.paginated.BackItem;
import me.zrageyh.telefono.items.paginated.ForwardItem;
import me.zrageyh.telefono.setting.SettingsMySQL;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.gui.structure.Structure;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class ServiceManager {

    private final Database database;
    private final CacheAbbonamento cacheAbbonamento;
    private final CacheContatti cacheContatti;
    private final CacheNumeri cacheNumeri;
    private final CacheChiamata cacheChiamata;
    private final CacheHistoryChiamate cacheHistoryChiamate;
    private final CacheHistoryMessaggi cacheHistoryMessaggi;
    private final HeadDatabaseAPI headDatabaseAPI;
    private final ItemManager itemManager;
    private final ExecutorService sharedExecutor;

    private volatile boolean initialized = false;
    private final Object initLock = new Object();

    public ServiceManager() {
        database = Database.getInstance();
        headDatabaseAPI = new HeadDatabaseAPI();
        itemManager = new ItemManager();
        sharedExecutor = Executors.newFixedThreadPool(4, r -> {
            final Thread t = new Thread(r, "Telefono-Cache-Worker");
            t.setDaemon(true);
            return t;
        });

        // Inizializza cache con executor condiviso
        cacheAbbonamento = new CacheAbbonamento(sharedExecutor);
        cacheContatti = new CacheContatti(sharedExecutor);
        cacheNumeri = new CacheNumeri(sharedExecutor);
        cacheChiamata = new CacheChiamata(sharedExecutor);
        cacheHistoryChiamate = new CacheHistoryChiamate(sharedExecutor);
        cacheHistoryMessaggi = new CacheHistoryMessaggi(sharedExecutor);

        // Servizi non più necessari
    }

    /* Inizializza tutti i servizi in modo asincrono */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            synchronized (initLock) {
                if (initialized) {
                    return;
                }

                try {
                    Common.log("&8&m-----------------------------------------------------");
                    Common.log("&9&lInizializzazione ServiceManager...");
                    SettingsMySQL.init();
                    initializeCaches();
                    loadGlobalIngredients();
                    initialized = true;
                    Common.log("&a&l✓ ServiceManager inizializzato con successo");
                    Common.log("&8&m-----------------------------------------------------");

                } catch (final Exception e) {
                    Common.error(e, "&c&l✗ Errore durante inizializzazione ServiceManager");
                    Common.log("&8&m-----------------------------------------------------");
                    throw new RuntimeException("ServiceManager initialization failed", e);
                }
            }
        });
    }

    /* Inizializza le cache */
    private void initializeCaches() {
        cacheAbbonamento.loadDataToCache();
        cacheContatti.loadDataToCache();
        cacheNumeri.loadDataToCache();
        cacheHistoryChiamate.loadDataToCache();
        cacheHistoryMessaggi.loadDataToCache();
    }

    // createRateLimitService rimosso - non più necessario

    /* Cache warming per performance ottimali */
    public CompletableFuture<Void> warmCaches() {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("ServiceManager not initialized"));
        }

        Common.log("Avvio cache warming...");
        final long startTime = System.currentTimeMillis();

        return CompletableFuture.allOf(
            cacheAbbonamento.warmCache(),
            cacheContatti.warmCache(),
            cacheNumeri.warmCache()
        ).thenRun(() -> {
            final long duration = System.currentTimeMillis() - startTime;
            Common.log("Cache warming completato in " + duration + "ms");
        }).exceptionally(throwable -> {
            Common.error(throwable, "Errore durante cache warming");
            return null;
        });
    }

    /* Shutdown ordinato di tutti i servizi */
    public void shutdown() {
        Common.log("Shutdown ServiceManager in corso...");



        // Shutdown cache
        shutdownCaches();

        // Shutdown database
        if (database != null) {
            database.shutdown();
            database.close();
        }

        // Shutdown shared executor
        shudown(sharedExecutor);

        initialized = false;
        Common.log("ServiceManager chiuso");
    }

    public static void shudown(final ExecutorService sharedExecutor) {
        if (sharedExecutor != null && !sharedExecutor.isShutdown()) {
            sharedExecutor.shutdown();
            try {
                if (!sharedExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    sharedExecutor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                sharedExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /* Shutdown delle cache con timeout */
    private void shutdownCaches() {
        try {
            CompletableFuture.allOf(
                CompletableFuture.runAsync(() ->{
                    cacheAbbonamento.shutdown();
                    cacheContatti.shutdown();
                    cacheNumeri.shutdown();
                    cacheChiamata.shutdown();
                    cacheHistoryChiamate.shutdown();
                    cacheHistoryMessaggi.shutdown();
                })
            ).get(5, TimeUnit.SECONDS);
        } catch (final Exception e) {
            Common.error(e, "Timeout durante shutdown cache");
        }
    }

    /* Verifica stato di salute dei servizi */
    public boolean isHealthy() {
        return initialized &&
               database.isConnectionHealthy() &&
               cacheAbbonamento != null;
    }

    private void loadGlobalIngredients() {
        Structure.addGlobalIngredient('#', getItemManager().getBorder());
        Structure.addGlobalIngredient('<', new BackItem());
        Structure.addGlobalIngredient('>', new ForwardItem());

        Structure.addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL);
        Structure.addGlobalIngredient('y', Markers.CONTENT_LIST_SLOT_VERTICAL);
    }
} 