package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class CacheAbbonamento {

    private final Cache<String, Abbonamento> cache;
    private final ExecutorService executorService;

    public CacheAbbonamento() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
        this.executorService = Executors.newCachedThreadPool();
    }

    public void update(final Abbonamento data) {
        cache.put(data.getSim(), data);
    }

    public CompletableFuture<Optional<Abbonamento>> get(final String sim) {
        Abbonamento cachedAbbonamento = cache.getIfPresent(sim);
        if (cachedAbbonamento != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedAbbonamento));
        }

        return CompletableFuture.supplyAsync(() -> {
            Optional<Abbonamento> abbonamento = Database.getInstance().getSubscription(sim);
            abbonamento.ifPresent(this::update);
            return abbonamento;
        }, executorService);
    }

}
