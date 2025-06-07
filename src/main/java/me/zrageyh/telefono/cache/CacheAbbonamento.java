package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Abbonamento;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class CacheAbbonamento implements CacheInterface<Abbonamento> {

    private final Cache<String, Abbonamento> cache;
    private final ExecutorService executor;

    public CacheAbbonamento(final ExecutorService sharedExecutor) {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
        executor = sharedExecutor;
    }

    public void update(final Abbonamento data) {
        cache.put(data.getSim(), data);
    }

    @Override
    public void update(final String sim, final int id, final Abbonamento data) {
        cache.put(sim, data);
    }

    @Override
    public void remove(final String sim, final int id) {
        cache.invalidate(sim);
    }

    public CompletableFuture<Optional<Abbonamento>> get(final String sim) {
        final Abbonamento cachedAbbonamento = cache.getIfPresent(sim);
        if (cachedAbbonamento != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedAbbonamento));
        }

        return Database.getInstance().getSubscription(sim)
                .thenApply(abbonamento -> {
                    abbonamento.ifPresent(this::update);
                    return abbonamento;
                });
    }

    @Override
    public void loadDataToCache() {
        Database.getInstance().getAllAbbonamenti()
                .thenAccept(allAbbonamenti -> {
                    allAbbonamenti.forEach(cache::put);
                });
    }

    /* Cache warming per performance */
    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(() -> {
            loadDataToCache();
        }, executor);
    }

    /* Batch update per performance migliori */
    public CompletableFuture<Void> updateBatch(final java.util.List<Abbonamento> abbonamenti) {
        return Database.getInstance().saveAllSubscriptions(abbonamenti)
                .thenRun(() -> {
                    abbonamenti.forEach(this::update);
                });
    }

    /* Cleanup per shutdown */
    public void shutdown() {
        ServiceManager.shudown(executor);
        cache.invalidateAll();
    }

    /* Statistiche cache */
    public void logCacheStats() {
        final var stats = cache.stats();
        System.out.println("CacheAbbonamento - Hit Rate: " + String.format("%.2f%%", stats.hitRate() * 100));
    }
}
