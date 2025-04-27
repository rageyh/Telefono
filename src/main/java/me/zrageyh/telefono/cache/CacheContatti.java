package me.zrageyh.telefono.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import me.zrageyh.telefono.model.Contatto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheContatti {

    @Getter
    private final Cache<String, List<Contatto>> cache;
    private final ExecutorService executorService;


    public CacheContatti() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
        executorService = Executors.newCachedThreadPool();
    }

    public void update(final Contatto data) {
        cache.getIfPresent(data.getSim()).removeIf((c) -> c.getId() == data.getId());
        cache.getIfPresent(data.getSim()).add(data);
    }

    public void put(final String sim, final Contatto data) {
        List<Contatto> contattoList = cache.getIfPresent(sim);

        if (contattoList == null || contattoList.isEmpty()) {
            contattoList = new ArrayList<>();
            contattoList.add(data);
            cache.put(sim, contattoList);
            return;
        }
        if (!contattoList.contains(data)) {
            contattoList.add(data);
        }

    }

    public void remove(final String sim, final int id) {
        cache.getIfPresent(sim).removeIf((c) -> c.getId() == id);
    }


    public boolean isSaved(final String sim, final String number) {
        return cache.getIfPresent(sim) != null && cache.getIfPresent(sim).stream().anyMatch((c) -> c.getNumber().equalsIgnoreCase(number));
    }


    public Optional<List<Contatto>> get(final String sim) {
        final List<Contatto> cachedContatti = cache.getIfPresent(sim);
        if (cachedContatti != null && !cachedContatti.isEmpty()) {
            return Optional.of(cachedContatti);
        }
        return Optional.empty();
    }

}

