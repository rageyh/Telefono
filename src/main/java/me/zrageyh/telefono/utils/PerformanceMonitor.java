package me.zrageyh.telefono.utils;

import lombok.Getter;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.manager.ServiceManager;
import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PerformanceMonitor {

    @Getter
    private static final PerformanceMonitor instance = new PerformanceMonitor();
    private ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    private boolean monitoring = false;

    private PerformanceMonitor() {
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public void startMonitoring() {
        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        if (!settings.getBoolean("performance.monitoring.enabled", true) || monitoring) {
            return;
        }

        monitoring = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "Telefono-Performance-Monitor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        final int intervalSeconds = settings.getInteger("performance.monitoring.intervalSeconds", 300);
        scheduler.scheduleAtFixedRate(this::collectMetrics, 30, intervalSeconds, TimeUnit.SECONDS);
        Common.log("Performance monitoring avviato (intervallo: " + intervalSeconds + "s)");
    }

    public void stopMonitoring() {
        if (!monitoring) {
            return;
        }

        monitoring = false;
        stop(scheduler);
        Common.log("Performance monitoring fermato");
    }

    private void stop(ExecutorService scheduler) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (final InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void collectMetrics() {
        try {
            final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
            final Map<String, Object> metrics = gatherMetrics();
            logMetrics(metrics);

            final double tps = (double) metrics.get("tps");
            final double memoryUsage = (double) metrics.get("memory_usage_percent");
            final double cacheHitRate = (double) metrics.get("avg_cache_hit_rate");

            final double warnTpsBelow = settings.getDouble("performance.monitoring.warnTpsBelow", 18.0);
            final double warnMemoryAbove = settings.getDouble("performance.monitoring.warnMemoryAbove", 85.0);
            final double warnCacheHitBelow = settings.getDouble("performance.monitoring.warnCacheHitBelow", 90.0);

            if (tps < warnTpsBelow) {
                Common.warning("TPS basso rilevato: " + String.format("%.2f", tps));
            }

            if (memoryUsage > warnMemoryAbove) {
                Common.warning("Uso memoria alto: " + String.format("%.1f%%", memoryUsage));
            }

            if (cacheHitRate < warnCacheHitBelow) {
                Common.warning("Cache hit rate basso: " + String.format("%.1f%%", cacheHitRate));
            }

        } catch (final Exception e) {
            Common.error(e, "Errore durante raccolta metriche performance");
        }
    }

    public Map<String, Object> gatherMetrics() {
        final Map<String, Object> metrics = new HashMap<>();

        final double tps = Math.min(20.0, Bukkit.getTPS()[0]);
        metrics.put("tps", tps);

        final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        final double memoryUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        metrics.put("memory_usage_percent", memoryUsagePercent);
        metrics.put("memory_used_mb", heapUsage.getUsed() / 1024 / 1024);
        metrics.put("memory_max_mb", heapUsage.getMax() / 1024 / 1024);

        final ServiceManager serviceManager = Telefono.getInstance().getServiceManager();
        if (serviceManager != null) {
            final double abbHitRate = serviceManager.getCacheAbbonamento().getCacheHitRate() * 100;
            final double contHitRate = serviceManager.getCacheContatti().getCacheHitRate() * 100;
            final double avgHitRate = (abbHitRate + contHitRate) / 2;

            metrics.put("cache_abbonamento_hit_rate", abbHitRate);
            metrics.put("cache_contatti_hit_rate", contHitRate);
            metrics.put("avg_cache_hit_rate", avgHitRate);

            metrics.put("cache_abbonamento_size", serviceManager.getCacheAbbonamento().getCacheSize());
            metrics.put("cache_contatti_size", serviceManager.getCacheContatti().getCacheSize());
        }

        metrics.put("players_online", Bukkit.getOnlinePlayers().size());

        return metrics;
    }

    private void logMetrics(final Map<String, Object> metrics) {
        Common.log("=== TELEFONO PERFORMANCE METRICS ===");
        Common.log("TPS: " + String.format("%.2f", (Double) metrics.get("tps")));
        Common.log("Memory: " + String.format("%.1f%% (%d/%d MB)",
            (Double) metrics.get("memory_usage_percent"),
            (Long) metrics.get("memory_used_mb"),
            (Long) metrics.get("memory_max_mb")));

        if (metrics.containsKey("avg_cache_hit_rate")) {
            Common.log("Cache Hit Rate: Abb=" + String.format("%.1f%%", (Double) metrics.get("cache_abbonamento_hit_rate")) +
                      ", Cont=" + String.format("%.1f%%", (Double) metrics.get("cache_contatti_hit_rate")));
            Common.log("Cache Size: Abb=" + metrics.get("cache_abbonamento_size") +
                      ", Cont=" + metrics.get("cache_contatti_size"));
        }

        Common.log("Players Online: " + metrics.get("players_online"));
        Common.log("===================================");
    }

    public boolean isMonitoring() {
        return monitoring;
    }
} 