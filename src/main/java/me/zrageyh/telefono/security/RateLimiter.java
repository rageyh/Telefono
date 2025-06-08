package me.zrageyh.telefono.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class RateLimiter {

    @Getter
    private static final RateLimiter instance = new RateLimiter();

    private final Map<UUID, PlayerLimits> playerLimits = new ConcurrentHashMap<>();

    final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");

    private RateLimiter() {}

    public boolean canPerformAction(final Player player, final ActionType action) {
        if (player == null) {
            return false;
        }


        if (!settings.getBoolean("security.rateLimiting.enabled", true) ||
            player.hasPermission("telefono.bypass.ratelimit")) {
            return true;
        }

        final PlayerLimits limits = playerLimits.computeIfAbsent(player.getUniqueId(),
            k -> new PlayerLimits());

        return limits.canPerform(action);
    }

    public void recordAction(final Player player, final ActionType action) {
        if (player == null) {
            return;
        }

        final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
        if (!settings.getBoolean("security.rateLimiting.enabled", true) ||
            player.hasPermission("telefono.bypass.ratelimit")) {
            return;
        }

        final PlayerLimits limits = playerLimits.computeIfAbsent(player.getUniqueId(),
            k -> new PlayerLimits());

        limits.recordAction(action);
    }

    public boolean attemptAction(final Player player, final ActionType action) {
        if (!canPerformAction(player, action)) {
            final long remainingCooldown = getRemainingCooldown(player, action);
            Messenger.error(player, "&cAzione troppo frequente! Riprova tra " +
                           (remainingCooldown / 1000) + " secondi");

            Common.log("[SECURITY] Rate limit hit - Player: " + player.getName() +
                      ", Action: " + action + ", Cooldown: " + remainingCooldown + "ms");
            return false;
        }

        recordAction(player, action);
        return true;
    }

    public long getRemainingCooldown(final Player player, final ActionType action) {
        if (player == null) {
            return 0;
        }

        final PlayerLimits limits = playerLimits.get(player.getUniqueId());
        if (limits == null) {
            return 0;
        }

        return limits.getRemainingCooldown(action);
    }

    public void clearPlayer(final UUID playerUuid) {
        playerLimits.remove(playerUuid);
    }

    public void cleanup() {
        final long now = System.currentTimeMillis();
        playerLimits.entrySet().removeIf(entry ->
            entry.getValue().isExpired(now));
    }

    public enum ActionType {
        SEND_MESSAGE("sendMessage"),
        MAKE_CALL("makeCall"),
        OPEN_CONTACTS("openContacts"),
        SAVE_CONTACT("saveContact"),
        DELETE_CONTACT("deleteContact"),
        OPEN_HISTORY("openHistory"),
        OPEN_TELEPHONE("openTelephone"),
        DATABASE_OPERATION("databaseOperation");

        private final String configKey;

        ActionType(final String configKey) {
            this.configKey = configKey;
        }

        public long getCooldownMs() {
            final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");
            if (!settings.getBoolean("security.rateLimiting.enabled", true)) {
                return 0;
            }

            final long defaultValue = switch (this) {
                case SEND_MESSAGE -> 2000;
                case MAKE_CALL -> 3000;
                case OPEN_CONTACTS -> 1000;
                case SAVE_CONTACT -> 5000;
                case DELETE_CONTACT -> 3000;
                case OPEN_HISTORY -> 2000;
                case OPEN_TELEPHONE -> 500;
                case DATABASE_OPERATION -> 1000;
            };

            return settings.getLong("security.rateLimiting." + configKey, defaultValue);
        }
    }

    private static final class PlayerLimits {
        private final Map<ActionType, Long> lastActionTimes = new ConcurrentHashMap<>();
        private static final long EXPIRE_TIME = TimeUnit.MINUTES.toMillis(10);

        boolean canPerform(final ActionType action) {
            final long now = System.currentTimeMillis();
            final Long lastAction = lastActionTimes.get(action);

            if (lastAction == null) {
                return true;
            }

            return (now - lastAction) >= action.getCooldownMs();
        }

        void recordAction(final ActionType action) {
            lastActionTimes.put(action, System.currentTimeMillis());
        }

        long getRemainingCooldown(final ActionType action) {
            final long now = System.currentTimeMillis();
            final Long lastAction = lastActionTimes.get(action);

            if (lastAction == null) {
                return 0;
            }

            final long elapsed = now - lastAction;
            final long cooldown = action.getCooldownMs();

            return Math.max(0, cooldown - elapsed);
        }

        boolean isExpired(final long now) {
            if (lastActionTimes.isEmpty()) {
                return true;
            }

            final long lastActivity = lastActionTimes.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

            return (now - lastActivity) > EXPIRE_TIME;
        }
    }
} 