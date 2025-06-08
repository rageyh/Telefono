package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.RedisManager;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Abbonamento;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public final class CacheAbbonamento implements CacheInterface<Abbonamento> {

    private final Cache<String, Abbonamento> l1Cache;
    private final ExecutorService executor;
    private final RedisManager redisManager;

    public CacheAbbonamento(final ExecutorService sharedExecutor) {
        executor = sharedExecutor;
        redisManager = RedisManager.getInstance();

        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        final int maxSize = settings.getInteger("cache.abbonamento.maxSize", 1000);
        final int expireWrite = settings.getInteger("cache.abbonamento.expireAfterWrite", 5);
        final int expireAccess = settings.getInteger("cache.abbonamento.expireAfterAccess", 2);

        l1Cache = Caffeine.newBuilder()
                .expireAfterWrite(expireWrite, TimeUnit.MINUTES)
                .expireAfterAccess(expireAccess, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted() && value != null) {
                        redisManager.cacheAbbonamento((String) key, value);
                    }
                })
                .build();
    }

    public void update(final Abbonamento data) {
        l1Cache.put(data.getSim(), data);
        redisManager.cacheAbbonamento(data.getSim(), data);
    }

    @Override
    public void update(final String sim, final int id, final Abbonamento data) {
        l1Cache.put(sim, data);
        redisManager.cacheAbbonamento(sim, data);
    }

    @Override
    public void remove(final String sim, final int id) {
        l1Cache.invalidate(sim);
        redisManager.invalidateAbbonamento(sim);
    }

    public CompletableFuture<Optional<Abbonamento>> get(final String sim) {
        final Abbonamento l1Cached = l1Cache.getIfPresent(sim);
        if (l1Cached != null) {
            return CompletableFuture.completedFuture(Optional.of(l1Cached));
        }

        return redisManager.getCachedAbbonamento(sim)
                .thenCompose(l2Cached -> {
                    if (l2Cached.isPresent()) {
                        try {
                            final Abbonamento abbonamento = parseAbbonamento(l2Cached.get());
                            l1Cache.put(sim, abbonamento);
                            return CompletableFuture.completedFuture(Optional.of(abbonamento));
                        } catch (final Exception e) {
                            Common.error(e, "Errore parsing Redis cache per abbonamento: " + sim);
                        }
                    }

                    return Database.getInstance().getSubscription(sim)
                            .thenApply(abbonamento -> {
                                abbonamento.ifPresent(this::update);
                                return abbonamento;
                            });
                });
    }

    @Override
    public void loadDataToCache() {
        Database.getInstance().getAllAbbonamenti()
                .thenAccept(allAbbonamenti -> {
                    allAbbonamenti.forEach((sim, abbonamento) -> {
                        l1Cache.put(sim, abbonamento);
                        redisManager.cacheAbbonamento(sim, abbonamento);
                    });
                    Common.log("Cache abbonamenti caricata: " + allAbbonamenti.size() + " elementi");
                });
    }

    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(this::loadDataToCache, executor);
    }

    public CompletableFuture<Void> updateBatch(final java.util.List<Abbonamento> abbonamenti) {
        return Database.getInstance().saveAllSubscriptions(abbonamenti)
                .thenRun(() -> abbonamenti.forEach(this::update));
    }

    public void shutdown() {
        final var stats = l1Cache.stats();
        Common.log("CacheAbbonamento shutdown - Hit Rate: " +
                   String.format("%.2f%%, Requests: %d", stats.hitRate() * 100, stats.requestCount()));

        ServiceManager.shudown(executor);
        l1Cache.invalidateAll();
    }

    private Abbonamento parseAbbonamento(final String json) {
        final var obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        return new Abbonamento(
            obj.get("sim").getAsString(),
            obj.get("abbonamento").getAsString(),
            obj.get("messages").getAsInt(),
            obj.get("calls").getAsInt()
        );
    }

    public double getCacheHitRate() {
        return l1Cache.stats().hitRate();
    }

    public long getCacheSize() {
        return l1Cache.estimatedSize();
    }

    // Compatibility method
    public Cache<String, Abbonamento> getCache() {
        return l1Cache;
    }
}
