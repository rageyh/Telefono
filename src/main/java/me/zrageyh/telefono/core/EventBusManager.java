package me.zrageyh.telefono.core;

import lombok.Getter;
import org.mineacademy.fo.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * ARCHITECTURE FIX: Event Bus per comunicazione inter-modulare
 */
public final class EventBusManager {

    @Getter
    private static final EventBusManager instance = new EventBusManager();

    // Thread-safe event listeners storage
    private final Map<Class<?>, List<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor;

    private EventBusManager() {
        asyncExecutor = null; // Sarà inizializzato durante setup
    }

    /**
     * Registra un listener per un tipo di evento specifico
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(final Class<T> eventType, final Consumer<T> listener) {
        eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                     .add((Consumer<Object>) listener);

        Common.log("Registered event listener for: " + eventType.getSimpleName());
    }

    /**
     * Pubblica un evento in modo sincrono
     */
    public void publish(final Object event) {
        if (event == null) return;

        final Class<?> eventType = event.getClass();
        final List<Consumer<Object>> listeners = eventListeners.get(eventType);

        if (listeners != null && !listeners.isEmpty()) {
            for (final Consumer<Object> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (final Exception e) {
                    Common.error(e, "Errore durante handle evento: " + eventType.getSimpleName());
                }
            }

            Common.log("Published event: " + eventType.getSimpleName() + " to " + listeners.size() + " listeners");
        }
    }

    /**
     * Rimuove tutti i listeners (cleanup)
     */
    public void clear() {
        eventListeners.clear();
        Common.log("EventBus cleared - all listeners removed");
    }
} 