package me.zrageyh.telefono.cache;

import me.zrageyh.telefono.manager.ServiceManager;
import me.zrageyh.telefono.model.Call;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheChiamata {

    private final Map<String, Call> calls = new HashMap<>();
    private final ExecutorService executor;


    public CacheChiamata(final ExecutorService sharedExecutor) {
        executor = sharedExecutor;
    }

    public void putData(final String simNumber, final Call call) {
        calls.put(simNumber, call);
    }

    public boolean containsNumber(final String number) {
        return calls.keySet().stream().anyMatch(key -> key.contains(number));
    }

    public Optional<Call> getData(final String number) {
        for (final String key : calls.keySet()) {
            if (key.contains(number)) {
                return Optional.of(calls.get(key));
            }
        }
        return Optional.empty();
    }

    public void removeData(final String number) {
        calls.keySet().removeIf(key -> key.contains(number));
    }

    public Map<String, Call> getActiveCalls() {
        return new HashMap<>(calls);
    }

    public void clearAll() {
        calls.clear();
    }

    /* Shutdown */
    public void shutdown() {
        ServiceManager.shudown(executor);
        clearAll();
    }
}
