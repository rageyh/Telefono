package me.zrageyh.telefono.manager;

import lombok.Getter;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.cache.*;
import me.zrageyh.telefono.core.EventBusManager;
import me.zrageyh.telefono.storage.PlayerDataManager;
import me.zrageyh.telefono.items.paginated.BackItem;
import me.zrageyh.telefono.items.paginated.ForwardItem;
import me.zrageyh.telefono.setting.SettingsMySQL;
import me.zrageyh.telefono.utils.PerformanceMonitor;
import me.zrageyh.telefono.security.SecurityManager;
import org.mineacademy.fo.settings.YamlConfig;
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
    private final CallBillingManager callBillingManager;
    private final RedisManager redisManager;
    private final EventBusManager eventBusManager;
    private final PlayerDataManager playerDataManager;
    private final ExecutorService sharedExecutor;

    private volatile boolean initialized = false;
    private final Object initLock = new Object();

    public ServiceManager() {
        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        final int coreSize = settings.getInteger("performance.threadPool.coreSize", 2);
        final int maxThreads = settings.getInteger("performance.threadPool.maxSize", 4);
        final int threadPoolSize = Math.min(Math.max(coreSize, 2), maxThreads);

        database = Database.getInstance();
        headDatabaseAPI = new HeadDatabaseAPI();
        itemManager = new ItemManager();
        callBillingManager = CallBillingManager.getInstance();
        redisManager = RedisManager.getInstance();
        eventBusManager = EventBusManager.getInstance();
        playerDataManager = PlayerDataManager.getInstance();
        sharedExecutor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            final Thread t = new Thread(r, "Telefono-Worker-" + System.currentTimeMillis() % 1000);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        cacheAbbonamento = new CacheAbbonamento(sharedExecutor);
        cacheContatti = new CacheContatti(sharedExecutor);
        cacheNumeri = new CacheNumeri(sharedExecutor);
        cacheChiamata = new CacheChiamata();
        cacheHistoryChiamate = new CacheHistoryChiamate(sharedExecutor);
        cacheHistoryMessaggi = new CacheHistoryMessaggi(sharedExecutor);

        Common.log("ServiceManager inizializzato con " + threadPoolSize + " worker threads");
    }

    /* Inizializza tutti i servizi nel main thread */
    public CompletableFuture<Void> initialize() {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        Common.runLater(1, () -> {
            synchronized (initLock) {
                if (initialized) {
                    future.complete(null);
                    return;
                }

                try {
                    SettingsMySQL.init();
                    if (!database.isConnectionHealthy()) {
                        Thread.sleep(2000);
                        if (!database.isConnectionHealthy()) {
                            throw new IllegalStateException("Database connection not available after initialization");
                        }
                    }

                    redisManager.initialize(sharedExecutor);
                    SecurityManager.getInstance().initialize();
                               initializeCaches();
                    loadGlobalIngredients();
                    initialized = true;
                    PerformanceMonitor.getInstance().startMonitoring();

                    future.complete(null);

                } catch (final Exception e) {
                    initialized = false;
                    final IllegalStateException exception = new IllegalStateException("ServiceManager initialization failed", e);
                    future.completeExceptionally(exception);
                }
            }
        });

        return future;
    }

    private void initializeCaches() {
        final long startTime = System.currentTimeMillis();

        CompletableFuture.allOf(
            CompletableFuture.runAsync(cacheAbbonamento::loadDataToCache, sharedExecutor),
            CompletableFuture.runAsync(cacheContatti::loadDataToCache, sharedExecutor),
            CompletableFuture.runAsync(cacheNumeri::loadDataToCache, sharedExecutor),
            CompletableFuture.runAsync(cacheHistoryChiamate::loadDataToCache, sharedExecutor),
            CompletableFuture.runAsync(cacheHistoryMessaggi::loadDataToCache, sharedExecutor)
        ).thenRun(() -> {
            final long duration = System.currentTimeMillis() - startTime;
            Common.log("        &7├─ Cache System: &a" + duration + "ms &7initialization time");
        });
    }

    /* Cache warming per performance ottimali */
    public CompletableFuture<Void> warmCaches() {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("ServiceManager not initialized"));
        }

        final long startTime = System.currentTimeMillis();

        return CompletableFuture.allOf(
            cacheAbbonamento.warmCache(),
            cacheContatti.warmCache(),
            cacheNumeri.warmCache()
        ).thenRun(() -> {
            final long duration = System.currentTimeMillis() - startTime;
            Common.log("        &7├─ Cache Warming: &a" + duration + "ms &7completion time");
        }).exceptionally(throwable -> {
            Common.error(throwable, "Errore durante cache warming");
            return null;
        });
    }

    /* Shutdown ordinato di tutti i servizi */
    public void shutdown() {
        Common.runLater(1, () -> {
            try {
                PerformanceMonitor.getInstance().stopMonitoring();
                SecurityManager.getInstance().shutdown();

                if (redisManager != null) {
                    redisManager.shutdown();
                }

                shutdownCaches();

                if (callBillingManager != null) {
                    callBillingManager.shutdown();
                }

                if (eventBusManager != null) {
                    eventBusManager.clear();
                }

                if (playerDataManager != null) {
                    playerDataManager.clearAllData();
                }

                if (database != null) {
                    database.shutdown();
                }

                shutdownExecutor();

                initialized = false;

            } catch (final Exception e) {
                Common.error(e, "Errore durante shutdown ServiceManager");
            }
        });
    }

    private void shutdownExecutor() {
        if (sharedExecutor != null && !sharedExecutor.isShutdown()) {
            sharedExecutor.shutdown();
            try {
                if (!sharedExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    sharedExecutor.shutdownNow();
                    if (!sharedExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        Common.warning("        &7├─ Thread Pool: &c&lFORCED SHUTDOWN");
                    } else {
                        Common.log("        &7├─ Thread Pool: &e&lFORCED CLOSURE");
                    }
                } else {
                    Common.log("        &7├─ Thread Pool: &a&lCLEAN SHUTDOWN");
                }
            } catch (final InterruptedException e) {
                sharedExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void shudown(final ExecutorService sharedExecutor) {
        PerformanceMonitor.stop(sharedExecutor);
    }

    /* Shutdown delle cache con timeout */
    private void shutdownCaches() {
        try {
            if (cacheAbbonamento != null) {
                cacheAbbonamento.shutdown();
            }
            if (cacheContatti != null) {
                cacheContatti.getCache().invalidateAll();
            }
            if (cacheNumeri != null) {
                cacheNumeri.getNumbers().clear();
            }
            if (cacheChiamata != null) {
                cacheChiamata.clearAll();
            }
            if (cacheHistoryChiamate != null) {
                cacheHistoryChiamate.getCache().invalidateAll();
            }
            if (cacheHistoryMessaggi != null) {
                cacheHistoryMessaggi.getCache().invalidateAll();
            }

            Common.log("        &7├─ Cache System: &a&lCLEARED");
        } catch (final Exception e) {
            Common.error(e, "Errore durante shutdown cache");
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

    /* Getter per CallBillingManager */
    public CallBillingManager getCallBillingManager() {
        return callBillingManager;
    }

    /* Getter per EventBusManager - ARCHITECTURE FIX Problema #4 */
    public EventBusManager getEventBusManager() {
        return eventBusManager;
    }

    /* Getter per PlayerDataManager - ARCHITECTURE FIX Problema #5 */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
} 