package me.zrageyh.telefono.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Manager Redis per cache L2 - Migliora performance riducendo miss rate su restart
 * PERFORMANCE FIX: Problema #2 - Cache multi-livello
 */
public final class RedisManager {

    @Getter
    private static final RedisManager instance = new RedisManager();
    private JedisPool jedisPool;
    private final Gson gson;
    private boolean enabled = false;
    private ExecutorService executor;

    // Cache TTL configurabili
    private static final int ABBONAMENTO_TTL = 3600; // 1 ora
    private static final int CONTATTI_TTL = 1800;    // 30 minuti
    private static final int NUMERI_TTL = 7200;      // 2 ore
    private static final int HISTORY_TTL = 86400;    // 24 ore

    // Redis key prefixes
    private static final String PREFIX_ABBONAMENTO = "tel:abb:";
    private static final String PREFIX_CONTATTI = "tel:cont:";
    private static final String PREFIX_NUMERI = "tel:num:";
    private static final String PREFIX_HISTORY_CALLS = "tel:hist:calls:";
    private static final String PREFIX_HISTORY_MSGS = "tel:hist:msgs:";

    private RedisManager() {
        this.gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .create();
        this.executor = null; // Sarà inizializzato durante initialize()
    }

    /**
     * Inizializza Redis basato sulla configurazione
     */
    public void initialize(final ExecutorService sharedExecutor) {
        this.executor = sharedExecutor;

        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        final boolean redisEnabled = settings.getBoolean("cache.redis.enabled", false);

        if (!redisEnabled) {
            Common.log("        &7├─ Redis Cache: &e&lDISABLED &7(L1 only mode)");
            return;
        }

        try {
            final String host = settings.getString("cache.redis.host", "localhost");
            final int port = settings.getInteger("cache.redis.port", 6379);
            final int timeout = settings.getInteger("cache.redis.timeout", 3000);
            final int maxTotal = settings.getInteger("cache.redis.maxConnections", 20);
            final int maxIdle = settings.getInteger("cache.redis.maxIdleConnections", 8);

            final JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(60000);
            poolConfig.setTimeBetweenEvictionRunsMillis(30000);
            poolConfig.setNumTestsPerEvictionRun(-1);
            poolConfig.setBlockWhenExhausted(true);
            poolConfig.setMaxWaitMillis(timeout);

            final String password = settings.getString("cache.redis.password", "");
            if (password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, timeout);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
            }

            try (final var jedis = jedisPool.getResource()) {
                jedis.ping();
                enabled = true;
                Common.log("        &7├─ Redis Cache: &a&lCONNECTED &7(L2 enabled)");
            }

        } catch (final Exception e) {
            Common.log("        &7├─ Redis Cache: &c&lFAILED &7(falling back to L1)");
            Common.log("        &7├─ Redis Error: &c" + e.getMessage());
            enabled = false;

            if (jedisPool != null) {
                jedisPool.close();
                jedisPool = null;
            }
        }
    }

    /**
     * Cache L2 per abbonamenti
     */
    public CompletableFuture<Optional<String>> getCachedAbbonamento(final String sim) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String cached = jedis.get(PREFIX_ABBONAMENTO + sim);
                return Optional.ofNullable(cached);
            } catch (final Exception e) {
                Common.error(e, "Errore recupero abbonamento Redis L2: " + sim);
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Void> cacheAbbonamento(final String sim, final Object abbonamento) {
        // CRITICAL FIX: Evita RejectedExecutionException durante shutdown
        if (!enabled || executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String json = gson.toJson(abbonamento);
                jedis.setex(PREFIX_ABBONAMENTO + sim, ABBONAMENTO_TTL, json);
            } catch (final Exception e) {
                // Solo log se non è durante shutdown
                if (!executor.isShutdown()) {
                    Common.error(e, "Errore cache abbonamento Redis L2: " + sim);
                }
            }
        }, executor).exceptionally(throwable -> {
            // Gestisce RejectedExecutionException silenziosamente durante shutdown
            if (!(throwable.getCause() instanceof java.util.concurrent.RejectedExecutionException)) {
                Common.error(throwable, "Errore imprevisto cache abbonamento Redis L2: " + sim);
            }
            return null;
        });
    }

    /**
     * Cache L2 per contatti
     */
    public CompletableFuture<Optional<String>> getCachedContatti(final String sim) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String cached = jedis.get(PREFIX_CONTATTI + sim);
                return Optional.ofNullable(cached);
            } catch (final Exception e) {
                Common.error(e, "Errore recupero contatti Redis L2: " + sim);
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Void> cacheContatti(final String sim, final List<?> contatti) {
        // CRITICAL FIX: Evita RejectedExecutionException durante shutdown
        if (!enabled || executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String json = gson.toJson(contatti);
                jedis.setex(PREFIX_CONTATTI + sim, CONTATTI_TTL, json);
            } catch (final Exception e) {
                // Solo log se non è durante shutdown
                if (!executor.isShutdown()) {
                    Common.error(e, "Errore cache contatti Redis L2: " + sim);
                }
            }
        }, executor).exceptionally(throwable -> {
            // Gestisce RejectedExecutionException silenziosamente durante shutdown
            if (!(throwable.getCause() instanceof java.util.concurrent.RejectedExecutionException)) {
                Common.error(throwable, "Errore imprevisto cache contatti Redis L2: " + sim);
            }
            return null;
        });
    }

    /**
     * Cache L2 per numeri SIM (raramente cambiano)
     */
    public CompletableFuture<Optional<String>> getCachedNumeri() {
        if (!enabled) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String cached = jedis.get(PREFIX_NUMERI + "all");
                return Optional.ofNullable(cached);
            } catch (final Exception e) {
                Common.error(e, "Errore recupero numeri Redis L2");
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Void> cacheNumeri(final List<String> numeri) {
        // CRITICAL FIX: Evita RejectedExecutionException durante shutdown
        if (!enabled || executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String json = gson.toJson(numeri);
                jedis.setex(PREFIX_NUMERI + "all", NUMERI_TTL, json);
            } catch (final Exception e) {
                // Solo log se non è durante shutdown
                if (!executor.isShutdown()) {
                    Common.error(e, "Errore cache numeri Redis L2");
                }
            }
        }, executor).exceptionally(throwable -> {
            // Gestisce RejectedExecutionException silenziosamente durante shutdown
            if (!(throwable.getCause() instanceof java.util.concurrent.RejectedExecutionException)) {
                Common.error(throwable, "Errore imprevisto cache numeri Redis L2");
            }
            return null;
        });
    }

    /**
     * Cache L2 per cronologia (meno critica, TTL più lungo)
     */
    public CompletableFuture<Optional<String>> getCachedHistoryChiamate(final String sim) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String cached = jedis.get(PREFIX_HISTORY_CALLS + sim);
                return Optional.ofNullable(cached);
            } catch (final Exception e) {
                Common.error(e, "Errore recupero history chiamate Redis L2: " + sim);
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Void> cacheHistoryChiamate(final String sim, final List<?> history) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String json = gson.toJson(history);
                jedis.setex(PREFIX_HISTORY_CALLS + sim, HISTORY_TTL, json);
            } catch (final Exception e) {
                Common.error(e, "Errore cache history chiamate Redis L2: " + sim);
            }
        }, executor);
    }

    /**
     * Invalidazione cache per aggiornamenti
     */
    public CompletableFuture<Void> invalidateAbbonamento(final String sim) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                jedis.del(PREFIX_ABBONAMENTO + sim);
            } catch (final Exception e) {
                Common.error(e, "Errore invalidazione abbonamento Redis L2: " + sim);
            }
        }, executor);
    }

    public CompletableFuture<Void> invalidateContatti(final String sim) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                jedis.del(PREFIX_CONTATTI + sim);
            } catch (final Exception e) {
                Common.error(e, "Errore invalidazione contatti Redis L2: " + sim);
            }
        }, executor);
    }

    /**
     * Statistiche cache per monitoring
     */
    public CompletableFuture<Map<String, Object>> getCacheStats() {
        if (!enabled) {
            return CompletableFuture.completedFuture(Map.of("enabled", false));
        }

        return CompletableFuture.supplyAsync(() -> {
            try (final var jedis = jedisPool.getResource()) {
                final String info = jedis.info("stats");
                final Map<String, Object> stats = Map.of(
                    "enabled", true,
                    "pool_active", jedisPool.getNumActive(),
                    "pool_idle", jedisPool.getNumIdle(),
                    "redis_info", info
                );
                return stats;
            } catch (final Exception e) {
                Common.error(e, "Errore recupero statistiche Redis");
                return Map.of("enabled", true, "error", e.getMessage());
            }
        }, executor);
    }

    /**
     * Shutdown pulito del Redis manager - SINCRONO nel main thread
     */
    public void shutdown() {
        // CRITICAL FIX: Esegue shutdown nel main thread per evitare problemi di concorrenza
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            performShutdown();
        } else {
            // Se non siamo nel main thread, schedula nel main thread
            org.mineacademy.fo.Common.runLater(() -> performShutdown());
        }
    }

    private void performShutdown() {
        enabled = false;

        if (jedisPool != null && !jedisPool.isClosed()) {
            try {
                jedisPool.close();
                Common.log("        &7├─ Redis Cache: &a&lDISCONNECTED");
            } catch (final Exception e) {
                Common.error(e, "Errore chiusura Redis JedisPool");
            }
        }

        executor = null;
    }

    public boolean isEnabled() {
        return enabled;
    }
} 