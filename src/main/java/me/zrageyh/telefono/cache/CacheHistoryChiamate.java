package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.history.HistoryChiamata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheHistoryChiamate {


    private final Cache<String, List<HistoryChiamata>> cache;
    private final ExecutorService executorService;


    public CacheHistoryChiamate() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
        this.executorService = Executors.newCachedThreadPool();
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

    public CompletableFuture<Optional<List<HistoryChiamata>>> get(final String sim) {
        final List<HistoryChiamata> cachedChiamate = cache.getIfPresent(sim);
        if (cachedChiamate != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedChiamate));
        }
        return CompletableFuture.supplyAsync(() -> {
            List<HistoryChiamata> chiamate = Database.getInstance().getHistoryChiamateBySim(sim);
            if (chiamate == null || chiamate.isEmpty()) {
                return Optional.empty();
            }
            for (final HistoryChiamata chiamata : chiamate) {
                cache.getIfPresent(sim).add(chiamata);
            }
            return Optional.of(chiamate);
        }, executorService);
    }

    public void update(final HistoryChiamata data) {
        cache.getIfPresent(data.getSim()).removeIf((c) -> c.getId() == data.getId());
        cache.getIfPresent(data.getSim()).add(data);
    }

    public void remove(final String sim, final int id) {
        cache.getIfPresent(sim).removeIf((c) -> c.getId() == id);
    }

}
