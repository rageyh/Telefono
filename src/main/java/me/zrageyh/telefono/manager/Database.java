package me.zrageyh.telefono.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.zrageyh.telefono.exp.SimNumberAlreadyExistsException;
import me.zrageyh.telefono.exp.SimSaveException;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.model.history.HistoryChiamata;
import me.zrageyh.telefono.model.history.HistoryMessaggio;
import me.zrageyh.telefono.utils.ValidationUtils;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.YamlConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CRITICAL FIX: Database completamente HikariCP-based
 * - Rimossa dipendenza da Foundation SimpleDatabase
 * - Tutte le query usano esclusivamente HikariCP
 * - Gestione connessioni ottimizzata e thread-safe
 */
public final class Database {
    @Getter
    private static final Database instance = new Database();

    private HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Database-Worker");
        t.setDaemon(true);
        return t;
    });

    private Database() {
        // Singleton
    }

    /**
     * CRITICAL FIX: Connessione database con HikariCP puro
     */
    public void connect(final String host, final int port, final String database,
                       final String username, final String password) {
        try {
            final String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            initConnectionPool(jdbcUrl, username, password);
            Common.log("        &7├─ Database: &a&lCONNECTED &7to MariaDB");
        } catch (final Exception e) {
            Common.error(e, "Errore connessione database");
            throw new RuntimeException("Impossibile connettersi al database", e);
        }
    }

    private void initConnectionPool(final String jdbcUrl, final String username, final String password) {
        try {
            final YamlConfig settings = YamlConfig.fromInternalPath("settings.yml");

            final HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.mariadb.jdbc.Driver");

            config.setMaximumPoolSize(settings.getInteger("database.connectionPool.maximum", 20));
            config.setMinimumIdle(settings.getInteger("database.connectionPool.minimum", 8));
            config.setConnectionTimeout(settings.getLong("database.connectionPool.connectionTimeout", 20000L));
            config.setIdleTimeout(settings.getLong("database.connectionPool.idleTimeout", 300000L));
            config.setMaxLifetime(settings.getLong("database.connectionPool.maxLifetime", 1200000L));
            config.setLeakDetectionThreshold(30000);
            config.setValidationTimeout(settings.getLong("database.connectionPool.validationTimeout", 3000L));
            config.setInitializationFailTimeout(settings.getLong("database.connectionPool.initFailTimeout", 10000L));

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("useLocalTransactionState", "true");
            config.addDataSourceProperty("autoReconnect", "true");
            config.addDataSourceProperty("failOverReadOnly", "false");
            config.addDataSourceProperty("maxReconnects", "3");

            dataSource = new HikariDataSource(config);

            Common.log("        &7├─ Connection Pool: &a" + config.getMaximumPoolSize() + " &7max connections");
        } catch (final Exception e) {
            Common.error(e, "Errore configurazione HikariCP");
            throw new RuntimeException("Impossibile inizializzare connection pool", e);
        }
    }

    /**
     * CRITICAL FIX: Solo HikariCP, nessun fallback Foundation
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool non disponibile");
        }

        final Connection conn = dataSource.getConnection();
        if (conn == null || !conn.isValid(5)) {
            throw new SQLException("Connessione database non valida");
        }

        return conn;
    }

    /**
     * Health check HikariCP
     */
    public boolean isConnectionHealthy() {
        try (final Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (final Exception e) {
            Common.error(e, "Health check database fallito");
            return false;
        }
    }

    /**
     * CRITICAL FIX: Creazione tabelle con HikariCP puro - SINCRONO nel main thread
     */
    public void initializeTables() {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            performTableInitialization();
        } else {
            org.mineacademy.fo.Common.runLater(() -> performTableInitialization());
        }
    }

    private void performTableInitialization() {
        try (final Connection conn = getConnection()) {
            createTablesIfNotExists(conn);
            Common.log("        &7├─ Database Schema: &a&lINITIALIZED");
        } catch (final SQLException e) {
            Common.error(e, "Errore inizializzazione tabelle");
            throw new RuntimeException(e);
        }
    }

    private void createTablesIfNotExists(final Connection conn) throws SQLException {
        final String[] createTableQueries = {
            """
            CREATE TABLE IF NOT EXISTS abbonamento (
                sim VARCHAR(8) NOT NULL PRIMARY KEY,
                abbonamento TEXT NOT NULL,
                messaggi_rimanenti INT NOT NULL,
                chiamate_rimanenti INT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,

            """
            CREATE TABLE IF NOT EXISTS sim (
                numero_sim VARCHAR(20) NOT NULL PRIMARY KEY
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,

            """
            CREATE TABLE IF NOT EXISTS contatto (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sim VARCHAR(8) NOT NULL,
                numero VARCHAR(8) NOT NULL,
                nome VARCHAR(15) NOT NULL,
                cognome VARCHAR(15) NOT NULL,
                INDEX idx_sim (sim),
                INDEX idx_numero (numero)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,

            """
            CREATE TABLE IF NOT EXISTS cronologia_chiamate (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sim VARCHAR(8) NOT NULL,
                numero_chiamato VARCHAR(8) NOT NULL,
                data_chiamata VARCHAR(20) NOT NULL,
                is_persa BOOLEAN NOT NULL,
                INDEX idx_sim (sim),
                INDEX idx_numero (numero_chiamato),
                INDEX idx_data (data_chiamata)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,

            """
            CREATE TABLE IF NOT EXISTS cronologia_messaggi (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sim VARCHAR(8) NOT NULL,
                numero_mittente VARCHAR(15) NOT NULL,
                messaggio LONGTEXT NOT NULL,
                data_messaggio VARCHAR(20) NOT NULL,
                persa BOOLEAN NOT NULL,
                INDEX idx_sim (sim),
                INDEX idx_mittente (numero_mittente),
                INDEX idx_data (data_messaggio)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };

        try (final Statement stmt = conn.createStatement()) {
            for (final String query : createTableQueries) {
                stmt.execute(query);
            }
        }
    }

    /**
     * Helper method per eseguire query SELECT con HikariCP
     */
    private <T> List<T> executeSelectQuery(final String sql, final Object[] params,
                                          final ResultSetMapper<T> mapper) throws SQLException {
        final List<T> results = new ArrayList<>();

        try (final Connection conn = getConnection();
             final PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    final Object param = params[i];
                    if (param instanceof String) {
                        stmt.setString(i + 1, (String) param);
                    } else if (param instanceof Integer) {
                        stmt.setInt(i + 1, (Integer) param);
                    } else if (param instanceof Long) {
                        stmt.setLong(i + 1, (Long) param);
                    } else if (param instanceof Boolean) {
                        stmt.setBoolean(i + 1, (Boolean) param);
                    } else if (param instanceof Double) {
                        stmt.setDouble(i + 1, (Double) param);
                    } else if (param == null) {
                        stmt.setNull(i + 1, java.sql.Types.VARCHAR);
                    } else {
                        stmt.setString(i + 1, param.toString());
                    }
                }
            }

            try (final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        }

        return results;
    }

    
    /**
     * Helper method per query INSERT/UPDATE/DELETE
     */
    private int executeUpdateQuery(final String sql, final Object... params) throws SQLException {
        try (final Connection conn = getConnection();
             final PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                final Object param = params[i];
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, java.sql.Types.VARCHAR);
                } else {
                    stmt.setString(i + 1, param.toString());
                }
            }

            return stmt.executeUpdate();
        }
    }

    /**
     * ABBONAMENTO - Operazioni completamente HikariCP
     */
    public CompletableFuture<Void> saveSubscription(final Abbonamento subscription) {
        return CompletableFuture.runAsync(() -> {
            if (!ValidationUtils.isValidSimNumber(subscription.getSim())) {
                throw new IllegalArgumentException("Invalid SIM number: " + subscription.getSim());
            }

            final String sql = """
                INSERT INTO abbonamento (sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    abbonamento = VALUES(abbonamento),
                    messaggi_rimanenti = VALUES(messaggi_rimanenti),
                    chiamate_rimanenti = VALUES(chiamate_rimanenti)
                """;

            try {
                executeUpdateQuery(sql,
                    ValidationUtils.sanitizeForDatabase(subscription.getSim()),
                    ValidationUtils.sanitizeForDatabase(subscription.getAbbonamento()),
                    subscription.getMessages(),
                    subscription.getCalls());
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando abbonamento: " + subscription.getSim());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Optional<Abbonamento>> getSubscription(final String sim) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtils.isValidSimNumber(sim)) {
                return Optional.empty();
            }

            final String sql = "SELECT sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti FROM abbonamento WHERE sim = ?";

            try {
                final List<Abbonamento> results = executeSelectQuery(sql, new Object[]{sim}, rs ->
                    new Abbonamento(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));

                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando abbonamento: " + sim);
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Map<String, Abbonamento>> getAllAbbonamenti() {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti FROM abbonamento";
            final Map<String, Abbonamento> results = new HashMap<>();

            try {
                final List<Abbonamento> abbonamenti = executeSelectQuery(sql, null, rs ->
                    new Abbonamento(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));

                abbonamenti.forEach(abb -> results.put(abb.getSim(), abb));
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando tutti gli abbonamenti");
            }

            return results;
        }, executor);
    }

    public CompletableFuture<Void> saveAllSubscriptions(final List<Abbonamento> abbonamenti) {
        return CompletableFuture.runAsync(() -> {
            final String sql = """
                INSERT INTO abbonamento (sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    messaggi_rimanenti = VALUES(messaggi_rimanenti),
                    chiamate_rimanenti = VALUES(chiamate_rimanenti)
                """;

            try (final Connection conn = getConnection();
                 final PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (final Abbonamento abbonamento : abbonamenti) {
                    stmt.setString(1, abbonamento.getSim());
                    stmt.setString(2, abbonamento.getAbbonamento());
                    stmt.setInt(3, abbonamento.getMessages());
                    stmt.setInt(4, abbonamento.getCalls());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (final SQLException e) {
                Common.error(e, "Errore batch save abbonamenti");
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> updateSubscription(final Abbonamento subscription) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "UPDATE abbonamento SET messaggi_rimanenti = ?, chiamate_rimanenti = ? WHERE sim = ?";
            try {
                executeUpdateQuery(sql, subscription.getMessages(), subscription.getCalls(), subscription.getSim());
            } catch (final SQLException e) {
                Common.error(e, "Errore aggiornando abbonamento: " + subscription.getSim());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * SIM - Operazioni HikariCP
     */
    public CompletableFuture<Void> saveSim(final String number) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO sim (numero_sim) VALUES (?)";

            try {
                final int affected = executeUpdateQuery(sql, number);
                if (affected == 0) {
                    throw new SimNumberAlreadyExistsException("Il numero SIM " + number + " esiste già.");
                }
            } catch (final SQLException e) {
                if (e.getErrorCode() == 1062) { // Duplicate entry
                    throw new SimNumberAlreadyExistsException("Il numero SIM " + number + " esiste già.");
                }
                throw new SimSaveException("Errore durante il salvataggio del numero SIM", e);
            }
        }, executor);
    }

    public CompletableFuture<List<String>> getAllSim() {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT numero_sim FROM sim";
            try {
                return executeSelectQuery(sql, null, rs -> rs.getString(1));
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando tutte le SIM");
                return new ArrayList<>();
            }
        }, executor);
    }

    /**
     * Contatti - Operazioni HikariCP
     */
    public CompletableFuture<Void> saveContatto(final Contatto contatto) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO contatto (sim, numero, nome, cognome) VALUES (?, ?, ?, ?)";
            try {
                executeUpdateQuery(sql, contatto.getSim(), contatto.getNumber(),
                                 contatto.getName(), contatto.getSurname());
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando contatto: " + contatto.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<Contatto>>> getAllContatti() {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT id, sim, numero, nome, cognome FROM contatto ORDER BY sim, nome";
            final Map<String, List<Contatto>> results = new HashMap<>();

            try {
                final List<Contatto> contatti = executeSelectQuery(sql, null, rs ->
                    new Contatto(rs.getInt(1), rs.getString(2), rs.getString(3),
                               rs.getString(4), rs.getString(5)));

                contatti.forEach(contatto -> {
                    results.computeIfAbsent(contatto.getSim(), k -> new ArrayList<>()).add(contatto);
                });
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando tutti i contatti");
            }

            return results;
        }, executor);
    }

    public CompletableFuture<List<Contatto>> getContattiBySim(final String sim) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT id, sim, numero, nome, cognome FROM contatto WHERE sim = ? ORDER BY nome";

            try {
                return executeSelectQuery(sql, new Object[]{sim}, rs ->
                    new Contatto(rs.getInt(1), rs.getString(2), rs.getString(3),
                               rs.getString(4), rs.getString(5)));
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando contatti per sim: " + sim);
                return new ArrayList<>();
            }
        }, executor);
    }

    public CompletableFuture<Void> updateContatto(final Contatto contatto) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "UPDATE contatto SET nome = ?, cognome = ? WHERE id = ?";
            try {
                executeUpdateQuery(sql, contatto.getName(), contatto.getSurname(), contatto.getId());
            } catch (final SQLException e) {
                Common.error(e, "Errore aggiornando contatto: " + contatto.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> deleteContatto(final int id) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "DELETE FROM contatto WHERE id = ?";
            try {
                executeUpdateQuery(sql, id);
            } catch (final SQLException e) {
                Common.error(e, "Errore eliminando contatto: " + id);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * Cronologia Messaggi - HikariCP puro
     */
    public CompletableFuture<Void> saveMessaggio(final HistoryMessaggio messaggio) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_messaggi (sim, numero_mittente, messaggio, data_messaggio, persa) VALUES (?, ?, ?, ?, ?)";
            try {
                executeUpdateQuery(sql, messaggio.getSim(), messaggio.getNumber(),
                                 messaggio.getMessage(), messaggio.getDate(), messaggio.isLost());
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando messaggio: " + messaggio.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<HistoryMessaggio>>> getAllHistoryMessaggi(final String input) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = """
                SELECT sim, numero_mittente, messaggio, data_messaggio, persa 
                FROM cronologia_messaggi 
                WHERE sim = ? OR numero_mittente = ? 
                ORDER BY data_messaggio DESC
                """;

            final Map<String, List<HistoryMessaggio>> results = new HashMap<>();
            final List<HistoryMessaggio> messaggi = new ArrayList<>();

            try {
                final List<HistoryMessaggio> allMessages = executeSelectQuery(sql, new Object[]{input, input}, rs -> {
                    final String sim = rs.getString(1);
                    final String mittente = rs.getString(2);
                    final String messaggio = rs.getString(3);
                    final String data = rs.getString(4);
                    final boolean isLost = rs.getBoolean(5);

                    if (input.equals(sim)) {
                        return new HistoryMessaggio(sim, mittente, data, messaggio, isLost, false);
                    } else {
                        return new HistoryMessaggio(mittente, sim, data, messaggio, isLost, true);
                    }
                });

                messaggi.addAll(allMessages);
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia messaggi per: " + input);
            }

            results.put(input, messaggi);
            return results;
        }, executor);
    }

    /**
     * Cronologia Chiamate - HikariCP puro
     */
    public CompletableFuture<Void> saveChiamata(final HistoryChiamata chiamata) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_chiamate (sim, numero_chiamato, data_chiamata, is_persa) VALUES (?, ?, ?, ?)";
            try {
                executeUpdateQuery(sql, chiamata.getSim(), chiamata.getNumber(),
                                 chiamata.getDate(), chiamata.isLost());
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando chiamata: " + chiamata.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<HistoryChiamata>>> getAllHistoryChiamate(final String input) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = """
                SELECT sim, numero_chiamato, data_chiamata, is_persa 
                FROM cronologia_chiamate 
                WHERE sim = ? OR numero_chiamato = ? 
                ORDER BY data_chiamata DESC
                """;

            final Map<String, List<HistoryChiamata>> results = new HashMap<>();
            final List<HistoryChiamata> chiamate = new ArrayList<>();

            try {
                final List<HistoryChiamata> allCalls = executeSelectQuery(sql, new Object[]{input, input}, rs -> {
                    final String sim = rs.getString(1);
                    final String numeroChiamato = rs.getString(2);
                    final String data = rs.getString(3);
                    final boolean isLost = rs.getBoolean(4);

                    if (input.equals(sim)) {
                        return new HistoryChiamata(sim, numeroChiamato, data, isLost, false);
                    } else {
                        return new HistoryChiamata(numeroChiamato, sim, data, isLost, true);
                    }
                });

                chiamate.addAll(allCalls);
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia chiamate per: " + input);
            }

            results.put(input, chiamate);
            return results;
        }, executor);
    }

    public CompletableFuture<List<HistoryChiamata>> getHistoryChiamateBySim(final String number) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = """
                SELECT sim, numero_chiamato, data_chiamata, is_persa 
                FROM cronologia_chiamate 
                WHERE sim = ? OR numero_chiamato = ? 
                ORDER BY data_chiamata DESC
                """;

            try {
                return executeSelectQuery(sql, new Object[]{number, number}, rs -> {
                    final String sim = rs.getString(1);
                    final String numeroChiamato = rs.getString(2);
                    final String data = rs.getString(3);
                    final boolean isLost = rs.getBoolean(4);

                    if (sim.equalsIgnoreCase(number)) {
                        return new HistoryChiamata(sim, numeroChiamato, data, isLost, false);
                    } else {
                        return new HistoryChiamata(numeroChiamato, sim, data, isLost, true);
                    }
                });
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia chiamate: " + number);
                return new ArrayList<>();
            }
        }, executor);
    }

    public CompletableFuture<List<HistoryMessaggio>> getHistoryMessaggiBySim(final String number) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = """
                SELECT sim, numero_mittente, messaggio, data_messaggio, persa 
                FROM cronologia_messaggi 
                WHERE sim = ? OR numero_mittente = ? 
                ORDER BY data_messaggio DESC
                """;

            try {
                return executeSelectQuery(sql, new Object[]{number, number}, rs -> {
                    final String sim = rs.getString(1);
                    final String mittente = rs.getString(2);
                    final String messaggio = rs.getString(3);
                    final String data = rs.getString(4);
                    final boolean isLost = rs.getBoolean(5);

                    if (sim.equalsIgnoreCase(number)) {
                        return new HistoryMessaggio(sim, mittente, data, messaggio, isLost, false);
                    } else {
                        return new HistoryMessaggio(mittente, sim, data, messaggio, isLost, true);
                    }
                });
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia messaggi: " + number);
                return new ArrayList<>();
            }
        }, executor);
    }

    /**
     * CRITICAL FIX: Shutdown completo HikariCP - SINCRONO nel main thread
     */
    public void shutdown() {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            performShutdown();
        } else {
            org.mineacademy.fo.Common.runLater(() -> performShutdown());
        }
    }

    private void performShutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        Common.warning("        &7├─ Database Pool: &c&lFORCED SHUTDOWN");
                    } else {
                        Common.log("        &7├─ Database Pool: &e&lFORCED CLOSURE");
                    }
                } else {
                    Common.log("        &7├─ Database Pool: &a&lCLEAN SHUTDOWN");
                }
            } catch (final InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                Common.log("        &7├─ HikariCP: &a&lDISCONNECTED");
            } catch (final Exception e) {
                Common.error(e, "Errore chiusura HikariCP DataSource");
            }
        }
    }

    /**
     * Functional interface per mapping ResultSet
     */
    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}

