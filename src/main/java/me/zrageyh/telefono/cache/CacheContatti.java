package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Contatto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheContatti implements CacheInterface<Contatto> {

    @Getter
    private final Cache<String, List<Contatto>> cache;
    private final ExecutorService executor;

    public CacheContatti(final ExecutorService sharedExecutor) {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
        executor = sharedExecutor;
    }

    public void update(final Contatto data) {
        final List<Contatto> contattoList = cache.getIfPresent(data.getSim());
        if (contattoList != null) {
            contattoList.removeIf(c -> c.getId() == data.getId());
            contattoList.add(data);
        }
    }

    @Override
    public void update(final String sim, final int id, final Contatto data) {
        final List<Contatto> contattoList = cache.getIfPresent(sim);
        if (contattoList != null) {
            contattoList.removeIf(c -> c.getId() == id);
            contattoList.add(data);
        }
    }

    public void put(final String sim, final Contatto data) {
        List<Contatto> contattoList = cache.getIfPresent(sim);

        if (contattoList == null) {
            contattoList = new ArrayList<>();
            contattoList.add(data);
            cache.put(sim, contattoList);
            return;
        }

        if (!contattoList.contains(data)) {
            contattoList.add(data);
        }
    }

    @Override
    public void remove(final String sim, final int id) {
        final List<Contatto> contattoList = cache.getIfPresent(sim);
        if (contattoList != null) {
            contattoList.removeIf(c -> c.getId() == id);
        }
    }

    public boolean isSaved(final String sim, final String number) {
        final List<Contatto> contattoList = cache.getIfPresent(sim);
        return contattoList != null && contattoList.stream()
                .anyMatch(c -> c.getNumber().equalsIgnoreCase(number));
    }

    public CompletableFuture<Optional<List<Contatto>>> get(final String sim) {
        final List<Contatto> cachedContatti = cache.getIfPresent(sim);
        if (cachedContatti != null && !cachedContatti.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.of(cachedContatti));
        }

        return Database.getInstance().getContattiBySim(sim)
                .thenApply(contatti -> {
                    if (!contatti.isEmpty()) {
                        cache.put(sim, contatti);
                        return Optional.of(contatti);
                    }
                    return Optional.empty();
                });
    }

    @Override
    public void loadDataToCache() {
        Database.getInstance().getAllContatti()
                .thenAccept(allContatti -> {
                    allContatti.forEach((sim, contatti) -> {
                        if (!contatti.isEmpty()) {
                            cache.put(sim, contatti);
                        }
                    });
                });
    }

    /* Cache warming per performance migliori all'avvio */
    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(() -> {
            loadDataToCache();
        }, executor);
    }

    /* Cleanup per shutdown */
    public void shutdown() {
        ServiceManager.shudown(executor);
        cache.invalidateAll();
    }

    /* Statistiche cache per monitoring */
    public void logCacheStats() {
        final var stats = cache.stats();
        System.out.println("CacheContatti - Hit Rate: " + String.format("%.2f%%", stats.hitRate() * 100));
    }
}

