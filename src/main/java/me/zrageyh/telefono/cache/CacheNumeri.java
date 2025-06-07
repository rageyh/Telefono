package me.zrageyh.telefono.cache;

import lombok.Getter;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.manager.ServiceManager;
import org.mineacademy.fo.Common;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class CacheNumeri {

    @Getter
    private final List<String> numbers = new LinkedList<>();
    private final ExecutorService executor;

    public CacheNumeri(final ExecutorService sharedExecutor) {
        executor = sharedExecutor;
        loadDataToCache();
    }

    /* Carica dati in cache */
    public void loadDataToCache() {
        Database.getInstance().getAllSim().thenAccept(list -> {
            synchronized(numbers) {
                numbers.clear();
                numbers.addAll(list);
            }
        });
    }

    /* Cache warming */
    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(() -> {
            loadDataToCache();
        }, executor);
    }

    /* Shutdown */
    public void shutdown() {
        ServiceManager.shudown(executor);
    }
}
