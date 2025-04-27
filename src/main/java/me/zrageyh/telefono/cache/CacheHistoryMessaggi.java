package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.history.HistoryMessaggio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CacheHistoryMessaggi {


    private final Cache<String, List<HistoryMessaggio>> cache;
    private final ExecutorService executorService;


    public CacheHistoryMessaggi() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
        this.executorService = Executors.newCachedThreadPool();
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


    public void remove(final String sim, final int id) {
        cache.getIfPresent(sim).removeIf((c) -> c.getId() == id);
    }

    public CompletableFuture<Optional<List<HistoryMessaggio>>> getData(final String sim) {
        final List<HistoryMessaggio> cachedChiamate = cache.getIfPresent(sim);
        if (cachedChiamate != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedChiamate));
        }
        return CompletableFuture.supplyAsync(() -> {
            final List<HistoryMessaggio> chiamate = Database.getInstance().getHistoryMessaggiBySim(sim);
            if (chiamate == null || chiamate.isEmpty()) {
                return Optional.empty();
            }
            for (final HistoryMessaggio chiamata : chiamate) {
                cache.getIfPresent(sim).add(chiamata);
            }
            return Optional.of(chiamate);
        }, executorService);
    }

    public CompletableFuture<Optional<List<HistoryMessaggio>>> getForNumber(final String sim, final String number) {
        return getData(sim).thenApplyAsync(optMessaggi -> {
            if (optMessaggi.isPresent()) {
                List<HistoryMessaggio> filteredMessaggi = optMessaggi.get().stream()
                        .filter(msg -> msg.getNumber().equals(number))
                        .collect(Collectors.toList());
                return filteredMessaggi.isEmpty() ? Optional.empty() : Optional.of(filteredMessaggi);
            }
            return Optional.empty();
        }, executorService);
    }

}
