package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.history.HistoryChiamata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class CacheHistoryChiamate implements CacheInterface<HistoryChiamata> {

    private final Cache<String, List<HistoryChiamata>> cache;
    private final ExecutorService executor;

    public CacheHistoryChiamate(final ExecutorService sharedExecutor) {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(8, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .maximumSize(300)
                .recordStats()
                .build();
        executor = sharedExecutor;
    }

    @Override
    public void update(final String sim, final int id, final HistoryChiamata data) {
        // Implementazione specifica per cronologia
    }

    @Override
    public void remove(final String sim, final int id) {
        cache.invalidate(sim);
    }

    @Override
    public void loadDataToCache() {
        // Caricamento lazy per cronologia
    }

    public CompletableFuture<Optional<List<HistoryChiamata>>> get(final String sim) {
        final List<HistoryChiamata> cached = cache.getIfPresent(sim);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return Database.getInstance().getHistoryChiamateBySim(sim)
                .thenApply(chiamate -> {
                    cache.put(sim, chiamate);
                    return Optional.of(chiamate);
                });
    }

    public void put(final String sim, final HistoryChiamata data) {
        List<HistoryChiamata> objectContattoList = cache.getIfPresent(sim);
        if (objectContattoList == null || objectContattoList.isEmpty()) {
            objectContattoList = new ArrayList<>();
            objectContattoList.add(data);
            cache.put(sim, objectContattoList);
        }
        if (!objectContattoList.contains(data)) {
            objectContattoList.add(data);
        }
        cache.put(sim, objectContattoList);
    }

    public void update(final HistoryChiamata data) {
        cache.getIfPresent(data.getSim()).removeIf((c) -> c.getId() == data.getId());
        cache.getIfPresent(data.getSim()).add(data);
    }
}
