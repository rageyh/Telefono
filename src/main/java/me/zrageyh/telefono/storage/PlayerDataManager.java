package me.zrageyh.telefono.storage;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ARCHITECTURE FIX: Foundation-style PlayerData storage
 * Risolve il Problema #5 - Storage Player Data Non Ottimale (Regola 15)
 */
public final class PlayerDataManager {

    @Getter
    private static final PlayerDataManager instance = new PlayerDataManager();

    // Thread-safe storage per player data
    private final Map<UUID, PlayerTelephoneData> playerDataStorage = new ConcurrentHashMap<>();

    private PlayerDataManager() {
        // Singleton
    }

    /**
     * Ottiene i dati telefono per un giocatore (con auto-creazione)
     */
    public PlayerTelephoneData getPlayerData(final Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public PlayerTelephoneData getPlayerData(final UUID playerUuid) {
        return playerDataStorage.computeIfAbsent(playerUuid, PlayerTelephoneData::new);
    }

    /**
     * Salva dati specifici per un giocatore
     */
    public void savePlayerData(final UUID playerUuid, final PlayerTelephoneData data) {
        if (data != null) {
            playerDataStorage.put(playerUuid, data);
        }
    }

    /**
     * ARCHITECTURE FIX: Sostituzione EventOpenTelephone.serializedMap
     * Gestisce il backup dell'inventario in modo thread-safe
     */
    public void backupInventory(final Player player, final ItemStack[] inventory) {
        final PlayerTelephoneData data = getPlayerData(player);

        // CRITICAL FIX: Previeni doppio backup
        if (data.getBackupInventory() != null &&
            System.currentTimeMillis() - data.getInventoryBackupTime() < 1000) {
            return;
        }

        data.setBackupInventory(inventory);
        data.setInventoryBackupTime(System.currentTimeMillis());

        // CRITICAL FIX: Marca come telefono aperto per delay intelligente
        data.setLastOpenedGui("telefono");

        Common.log("Backup inventario per player: " + player.getName());
    }

    /**
     * Ripristina l'inventario del giocatore se disponibile
     * CRITICAL FIX: Con delay intelligente per GUI telefono
     */
    public boolean restoreInventory(final Player player) {
        final PlayerTelephoneData data = getPlayerData(player);
        final ItemStack[] backup = data.getBackupInventory();

        if (backup != null) {
            // CRITICAL FIX: Delay intelligente per telefono (ridotto a 100ms per prevenire doppi restore)
            final long timeSinceBackup = System.currentTimeMillis() - data.getInventoryBackupTime();
            final boolean isRecentTelephoneBackup = "telefono".equals(data.getLastOpenedGui()) &&
                                                   timeSinceBackup < 100; // 100ms contro doppi click

            if (isRecentTelephoneBackup) {
                Common.log("Restore inventario ignorato (telefono aperto di recente) per player: " + player.getName());
                return false;
            }

            player.getInventory().setContents(backup);
            data.clearBackupInventory(); // Clear dopo ripristino
            data.setLastOpenedGui(null); // Clear GUI tracking

            Common.log("Inventario ripristinato per player: " + player.getName());
            return true;
        }

        return false;
    }

    /**
     * CRITICAL FIX: Forza il restore dell'inventario (per quit/close definitivi)
     */
    public boolean forceRestoreInventory(final Player player) {
        final PlayerTelephoneData data = getPlayerData(player);
        final ItemStack[] backup = data.getBackupInventory();

        if (backup != null) {
            player.getInventory().setContents(backup);
            data.clearBackupInventory();
            data.setLastOpenedGui(null);

            Common.log("Inventario FORZATO ripristinato per player: " + player.getName());
            return true;
        }

        return false;
    }

    /**
     * Cleanup automatico per giocatori che si disconnettono
     */
    public void cleanupPlayerData(final UUID playerUuid) {
        final PlayerTelephoneData data = playerDataStorage.get(playerUuid);
        if (data != null) {
            // Clear dati temporanei ma mantiene dati persistenti
            data.clearTemporaryData();

            // Rimuovi completamente se non ha dati persistenti
            if (!data.hasPersistentData()) {
                playerDataStorage.remove(playerUuid);
                Common.log("Player data cleanup per UUID: " + playerUuid);
            }
        }
    }

    /**
     * Cleanup completo (shutdown plugin)
     */
    public void clearAllData() {
        playerDataStorage.clear();
        Common.log("PlayerDataManager: Tutti i dati player rimossi");
    }

    /**
     * Statistiche per monitoring
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "total_players", playerDataStorage.size(),
            "players_with_backup", playerDataStorage.values().stream()
                .mapToInt(data -> data.getBackupInventory() != null ? 1 : 0)
                .sum()
        );
    }

    /**
     * Classe per i dati telefono specifici del giocatore
     */
    public static final class PlayerTelephoneData {

        /**
         * -- GETTER --
         *  UUID del giocatore
         */
        @Getter
        private final UUID playerUuid;

        // Inventory backup data
        @Getter
        private ItemStack[] backupInventory;
        @Setter
        @Getter
        private long inventoryBackupTime;

        // Call state data
        @Getter
        @Setter
        private String currentCallSim;
        private long callStartTime;
        @Getter
        private boolean inCall;

        // UI state data
        @Getter
        private String lastOpenedGui;
        @Getter
        private long lastInteractionTime;

        @Setter
        @Getter
        private boolean notificationsEnabled = true;
        @Setter
        @Getter
        private boolean autoAnswerEnabled = false;

        PlayerTelephoneData(final UUID playerUuid) {
            this.playerUuid = playerUuid;
            this.lastInteractionTime = System.currentTimeMillis();
        }

        // ============ INVENTORY BACKUP METHODS ============

        public void setBackupInventory(final ItemStack[] inventory) {
            this.backupInventory = inventory != null ? inventory.clone() : null;
        }

        public void clearBackupInventory() {
            this.backupInventory = null;
            this.inventoryBackupTime = 0;
        }

        // ============ CALL STATE METHODS ============

        public void setInCall(final boolean inCall) {
            this.inCall = inCall;
            if (inCall) {
                this.callStartTime = System.currentTimeMillis();
            } else {
                this.callStartTime = 0;
                this.currentCallSim = null;
            }
        }

        public long getCallDuration() {
            return inCall ? System.currentTimeMillis() - callStartTime : 0;
        }

        // ============ UI STATE METHODS ============

        public void setLastOpenedGui(final String guiName) {
            this.lastOpenedGui = guiName;
            updateInteractionTime();
        }

        public void updateInteractionTime() {
            this.lastInteractionTime = System.currentTimeMillis();
        }

        // ============ SETTINGS METHODS ============

        // ============ UTILITY METHODS ============

        /**
         * Verifica se ha dati che devono essere persistiti
         */
        public boolean hasPersistentData() {
            return currentCallSim != null ||
                   !notificationsEnabled ||
                   autoAnswerEnabled;
        }

        /**
         * Rimuove dati temporanei mantenendo quelli persistenti
         */
        public void clearTemporaryData() {
            clearBackupInventory();
            setLastOpenedGui(null);
            // Non clear call state se in chiamata attiva
            if (!inCall) {
                setCurrentCallSim(null);
                callStartTime = 0;
            }
        }

    }
} 