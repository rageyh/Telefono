package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.history.HistoryMessaggio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class CacheHistoryMessaggi implements CacheInterface<HistoryMessaggio> {

    private final Cache<String, List<HistoryMessaggio>> cache;
    private final ExecutorService executor;

    public CacheHistoryMessaggi(final ExecutorService sharedExecutor) {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(8, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .maximumSize(300)
                .recordStats()
                .build();
        executor = sharedExecutor;
    }

    @Override
    public void update(final String sim, final int id, final HistoryMessaggio data) {
        // Implementazione specifica per cronologia messaggi
    }

    @Override
    public void remove(final String sim, final int id) {
        cache.invalidate(sim);
    }

    @Override
    public void loadDataToCache() {
        // Caricamento lazy per cronologia
    }

    public CompletableFuture<Optional<List<HistoryMessaggio>>> get(final String sim) {
        final List<HistoryMessaggio> cached = cache.getIfPresent(sim);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return Database.getInstance().getHistoryMessaggiBySim(sim)
                .thenApply(messaggi -> {
                    cache.put(sim, messaggi);
                    return Optional.of(messaggi);
                });
    }

    public void shutdown() {
        ServiceManager.shudown(executor);
        cache.invalidateAll();
    }

    public void update(final HistoryMessaggio data) {
        cache.getIfPresent(data.getSim()).removeIf((c) -> c.getId() == data.getId());
        cache.getIfPresent(data.getSim()).add(data);
    }

    public void put(final String sim, final HistoryMessaggio data) {
        List<HistoryMessaggio> objectContattoList = cache.getIfPresent(sim);
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

    public CompletableFuture<Optional<List<HistoryMessaggio>>> getForNumber(final String sim, final String number) {
        return get(sim).thenApplyAsync(optMessaggi -> {
            if (optMessaggi.isPresent()) {
                final List<HistoryMessaggio> filteredMessaggi = optMessaggi.get().stream()
                        .filter(msg -> msg.getNumber().equals(number))
                        .collect(Collectors.toList());
                return filteredMessaggi.isEmpty() ? Optional.empty() : Optional.of(filteredMessaggi);
            }
            return Optional.empty();
        }, executor);
    }
}
