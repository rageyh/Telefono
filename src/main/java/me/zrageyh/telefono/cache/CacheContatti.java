package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.RedisManager;
import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Contatto;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public final class CacheContatti implements CacheInterface<Contatto> {

    private final Cache<String, List<Contatto>> l1Cache;
    private final ExecutorService executor;
    private final RedisManager redisManager;
    private final Gson gson;

    public CacheContatti(final ExecutorService sharedExecutor) {
        executor = sharedExecutor;
        redisManager = RedisManager.getInstance();
        gson = new Gson();

        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        final int maxSize = settings.getInteger("cache.contatti.maxSize", 800);
        final int expireWrite = settings.getInteger("cache.contatti.expireAfterWrite", 8);
        final int expireAccess = settings.getInteger("cache.contatti.expireAfterAccess", 3);

        l1Cache = Caffeine.newBuilder()
                .expireAfterWrite(expireWrite, TimeUnit.MINUTES)
                .expireAfterAccess(expireAccess, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted() && value != null) {
                        redisManager.cacheContatti((String) key, (List<?>) value);
                    }
                })
                .build();
    }

    @Override
    public void update(final String sim, final int id, final Contatto data) {
        l1Cache.asMap().computeIfPresent(sim, (k, contatti) -> {
            contatti.removeIf(c -> c.getId() == id);
            contatti.add(data);
            redisManager.cacheContatti(sim, contatti);
            return contatti;
        });
    }

    @Override
    public void remove(final String sim, final int id) {
        l1Cache.asMap().computeIfPresent(sim, (k, contatti) -> {
            contatti.removeIf(c -> c.getId() == id);
            redisManager.cacheContatti(sim, contatti);
            return contatti;
        });
    }

    public CompletableFuture<Optional<List<Contatto>>> get(final String sim) {
        final List<Contatto> l1Cached = l1Cache.getIfPresent(sim);
        if (l1Cached != null) {
            return CompletableFuture.completedFuture(Optional.of(l1Cached));
        }

        return redisManager.getCachedContatti(sim)
                .thenCompose(l2Cached -> {
                    if (l2Cached.isPresent()) {
                        try {
                            final List<Contatto> contatti = gson.fromJson(l2Cached.get(),
                                new TypeToken<List<Contatto>>(){}.getType());
                            l1Cache.put(sim, contatti);
                            return CompletableFuture.completedFuture(Optional.of(contatti));
                        } catch (final Exception e) {
                            Common.error(e, "Errore parsing Redis cache per contatti: " + sim);
                        }
                    }

                    return Database.getInstance().getContattiBySim(sim)
                            .thenApply(contatti -> {
                                l1Cache.put(sim, contatti);
                                redisManager.cacheContatti(sim, contatti);
                                return Optional.of(contatti);
                            });
                });
    }

    @Override
    public void loadDataToCache() {
        Database.getInstance().getAllContatti()
                .thenAccept(allContatti -> {
                    allContatti.forEach((sim, contatti) -> {
                        l1Cache.put(sim, contatti);
                        redisManager.cacheContatti(sim, contatti);
                    });
                    Common.log("Cache contatti caricata: " + allContatti.size() + " SIM");
                });
    }

    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(this::loadDataToCache, executor);
    }

    public void shutdown() {
        final var stats = l1Cache.stats();
        Common.log("CacheContatti shutdown - Hit Rate: " +
                   String.format("%.2f%%, Size: %d", stats.hitRate() * 100, l1Cache.estimatedSize()));

        ServiceManager.shudown(executor);
        l1Cache.invalidateAll();
    }

    public double getCacheHitRate() {
        return l1Cache.stats().hitRate();
    }

    public long getCacheSize() {
        return l1Cache.estimatedSize();
    }

    // Compatibility methods
    public Cache<String, List<Contatto>> getCache() {
        return l1Cache;
    }

    public void update(final Contatto data) {
        final List<Contatto> contattoList = l1Cache.getIfPresent(data.getSim());
        if (contattoList != null) {
            contattoList.removeIf(c -> c.getId() == data.getId());
            contattoList.add(data);
            redisManager.cacheContatti(data.getSim(), contattoList);
        }
    }

    public void put(final String sim, final Contatto data) {
        l1Cache.asMap().compute(sim, (k, current) -> {
            if (current == null) {
                current = new java.util.ArrayList<>();
            }
            if (!current.contains(data)) {
                current.add(data);
                redisManager.cacheContatti(sim, current);
            }
            return current;
        });
    }

    public boolean isSaved(final String sim, final String number) {
        final List<Contatto> contattoList = l1Cache.getIfPresent(sim);
        return contattoList != null && contattoList.stream()
                .anyMatch(c -> c.getNumber().equalsIgnoreCase(number));
    }
}

