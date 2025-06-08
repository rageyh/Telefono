package me.zrageyh.telefono.security;

import lombok.Getter;
import me.zrageyh.telefono.utils.PerformanceMonitor;
import me.zrageyh.telefono.utils.ValidationUtils;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SecurityManager {

    @Getter
    private static final SecurityManager instance = new SecurityManager();

    private final RateLimiter rateLimiter;
    private ScheduledExecutorService cleanupExecutor;
    private boolean initialized = false;

    private SecurityManager() {
        rateLimiter = RateLimiter.getInstance();
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "Telefono-Security-Cleanup");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        cleanupExecutor.scheduleAtFixedRate(rateLimiter::cleanup, 5, 5, TimeUnit.MINUTES);

        initialized = true;
        Common.log("SecurityManager inizializzato");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        PerformanceMonitor.stop(cleanupExecutor);

        initialized = false;
        Common.log("SecurityManager chiuso");
    }

    public boolean validateAndAttemptAction(final Player player, final RateLimiter.ActionType action,
                                           final String input, final String inputType) {
        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");

        if (!ValidationUtils.isPlayerInValidState(player)) {
            if (settings.getBoolean("security.logging.securityEvents", true)) {
                ValidationUtils.logSecurityEvent(player, "INVALID_PLAYER_STATE", "Action: " + action);
            }
            return false;
        }

        if (settings.getBoolean("security.rateLimiting.enabled", true) &&
            !rateLimiter.attemptAction(player, action)) {
            if (settings.getBoolean("security.logging.securityEvents", true)) {
                ValidationUtils.logSecurityEvent(player, "RATE_LIMIT_EXCEEDED", "Action: " + action);
            }
            return false;
        }

        if (input != null && settings.getBoolean("security.validation.strictMode", true) &&
            !validateInput(player, input, inputType)) {
            if (settings.getBoolean("security.logging.securityEvents", true)) {
                ValidationUtils.logSecurityEvent(player, "INVALID_INPUT",
                    "Type: " + inputType + ", Input length: " + input.length());
            }
            return false;
        }

        return true;
    }

    private boolean validateInput(final Player player, final String input, final String inputType) {
        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");

        if (settings.getBoolean("security.validation.blockSqlInjection", true) &&
            ValidationUtils.containsSqlInjection(input)) {
            if (settings.getBoolean("security.logging.securityEvents", true)) {
                ValidationUtils.logSecurityEvent(player, "SQL_INJECTION_ATTEMPT",
                    "Type: " + inputType + ", Pattern detected");
            }
            return false;
        }

        switch (inputType.toLowerCase()) {
            case "sim":
                return ValidationUtils.isValidSimNumber(input);
            case "phone":
                return ValidationUtils.isValidPhoneNumber(input);
            case "name":
                return ValidationUtils.isValidContactName(input);
            case "message":
                return ValidationUtils.isValidMessage(input);
            default:
                return !input.trim().isEmpty() && input.length() <= 500;
        }
    }

    public boolean validatePermissions(final Player player, final String permission) {
        if (!ValidationUtils.hasValidPermissions(player, permission)) {
            ValidationUtils.logSecurityEvent(player, "PERMISSION_DENIED",
                "Required: " + permission);
            return false;
        }
        return true;
    }

    public boolean validateDatabaseOperation(final Player player, final String operation,
                                           final String... parameters) {
        if (!rateLimiter.attemptAction(player, RateLimiter.ActionType.DATABASE_OPERATION)) {
            ValidationUtils.logSecurityEvent(player, "DATABASE_RATE_LIMIT",
                "Operation: " + operation);
            return false;
        }

        for (final String param : parameters) {
            if (param != null && ValidationUtils.containsSqlInjection(param)) {
                ValidationUtils.logSecurityEvent(player, "DATABASE_INJECTION_ATTEMPT",
                    "Operation: " + operation + ", Parameter: " + param.substring(0, Math.min(param.length(), 50)));
                return false;
            }
        }

        return true;
    }

    public String sanitizeForLog(final String input) {
        if (input == null) {
            return "null";
        }

        return ValidationUtils.sanitizeInput(input)
                .substring(0, Math.min(input.length(), 100)) +
                (input.length() > 100 ? "..." : "");
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public boolean isInitialized() {
        return initialized;
    }
} 