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
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.database.SimpleDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Database extends SimpleDatabase {
    @Getter
    private static final Database instance = new Database();
    @Getter
    private int pingTicks;

    private HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private Database() {
        pingTicks = 0;
    }

    /* Inizializza HikariCP connection pool */
    public void initConnectionPool(final String jdbcUrl, final String username, final String password) {
        try {
            // Explicitly load the MariaDB driver
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Common.error(e, "Failed to load MariaDB driver");
            throw new RuntimeException("Failed to load MariaDB driver", e);
        }

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(12);
        config.setMinimumIdle(4);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                if (dataSource != null && !dataSource.isClosed()) {
                    final Connection conn = dataSource.getConnection();
                    if (conn != null && conn.isValid(5)) {
                        return conn;
                    }
                }
                return super.getConnection();

            } catch (final SQLException e) {
                retryCount++;
                Common.error(e, "Errore connessione database - tentativo " + retryCount + "/" + MAX_RETRIES);

                if (retryCount >= MAX_RETRIES) {
                    Common.error(e, "Connessione database fallita definitivamente dopo " + MAX_RETRIES + " tentativi");
                    return null;
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS * retryCount); // Exponential backoff
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Common.error(ie, "Interruzione durante retry connessione database");
                    return null;
                }
            }
        }
        return null;
    }

    /* Verifica connessione con fallback graceful */
    public boolean isConnectionHealthy() {
        try (final Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (final Exception e) {
            Common.error(e, "Health check database fallito");
            return false;
        }
    }

    @Override
    protected void onConnected() {
        final long now = System.currentTimeMillis();

        createTable(TableCreator.of("abbonamento")
                .addNotNull("sim", "VARCHAR(8)")
                .addNotNull("abbonamento", "text")
                .addNotNull("messaggi_rimanenti", "INT")
                .addNotNull("chiamate_rimanenti", "INT")
                .setPrimaryColumn("sim"));

        createTable(TableCreator.of("sim")
                .addNotNull("numero_sim", "VARCHAR(20)")
                .setPrimaryColumn("numero_sim"));

        createTable(TableCreator.of("contatto")
                .addNotNull("id", "INT AUTO_INCREMENT")
                .addNotNull("sim", "VARCHAR(8)")
                .addNotNull("numero", "VARCHAR(8)")
                .addNotNull("nome", "VARCHAR(15)")
                .addNotNull("cognome", "VARCHAR(15)")
                .setPrimaryColumn("id"));

        createTable(TableCreator.of("cronologia_chiamate")
                .addNotNull("id", "INT AUTO_INCREMENT")
                .addNotNull("sim", "VARCHAR(8)")
                .addNotNull("numero_chiamato", "VARCHAR(8)")
                .addNotNull("data_chiamata", "VARCHAR(20)")
                .addNotNull("is_persa", "BOOLEAN")
                .setPrimaryColumn("id"));

        createTable(TableCreator.of("cronologia_messaggi")
                .addNotNull("id", "INT AUTO_INCREMENT")
                .addNotNull("sim", "VARCHAR(8)")
                .addNotNull("numero_mittente", "VARCHAR(15)")
                .addNotNull("messaggio", "LONGTEXT")
                .addNotNull("data_messaggio", "VARCHAR(20)")
                .addNotNull("persa", "BOOLEAN")
                .setPrimaryColumn("id"));

        updatePing(now);
    }

    private void updatePing(final long oldTime) {
        pingTicks = (int) MathUtil.ceiling((double) (System.currentTimeMillis() - oldTime) / 1000.0 * 50.0 * 1.3);
        if (System.currentTimeMillis() - oldTime > 250L) {
            Common.warning("La connessione al database è lenta (" + MathUtil.formatTwoDigits((double) (System.currentTimeMillis() - oldTime) / 1000.0) + " secondi). Cerca di ridurre il carico e usare il database in localhost.");
            return;
        }
        Common.log("Database MySQL creato in: " + pingTicks + " secondi");
    }

    /* Operazione async con retry logic */
    private <T> CompletableFuture<T> executeWithRetry(final String operation, final CompletableFuture<T> future) {
        return future.handle((result, throwable) -> {
            if (throwable != null) {
                return retryOperation(operation, future, 1);
            }
            return CompletableFuture.completedFuture(result);
        }).thenCompose(f -> f);
    }

    private <T> CompletableFuture<T> retryOperation(final String operation, final CompletableFuture<T> original, final int attempt) {
        if (attempt > MAX_RETRIES) {
            Common.log("Operazione fallita dopo " + MAX_RETRIES + " tentativi: " + operation);
            return CompletableFuture.failedFuture(new RuntimeException("Max retries exceeded"));
        }

        Common.log("Retry tentativo " + attempt + " per: " + operation);

        return original.handle((result, throwable) -> {
            if (throwable != null) {
                return retryOperation(operation, original, attempt + 1);
            }
            return CompletableFuture.completedFuture(result);
        }).thenCompose(f -> f);
    }

    /**
     * ABBONAMENTO - Operazioni completamente asincrone
     */

    public CompletableFuture<Void> saveSubscription(final Abbonamento subscription) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO abbonamento (sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE abbonamento = ?, messaggi_rimanenti = ?, chiamate_rimanenti = ?";

            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, subscription.getSim());
                statement.setString(2, subscription.getAbbonamento());
                statement.setInt(3, subscription.getMessages());
                statement.setInt(4, subscription.getCalls());
                statement.setString(5, subscription.getAbbonamento());
                statement.setInt(6, subscription.getMessages());
                statement.setInt(7, subscription.getCalls());
                statement.executeUpdate();

            } catch (final SQLException e) {
                Common.error(e, "Errore salvando abbonamento: " + subscription.getSim());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Optional<Abbonamento>> getSubscription(final String sim) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT * FROM abbonamento WHERE sim = ?";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, sim);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(new Abbonamento(sim, rs.getString(2), rs.getInt(3), rs.getInt(4)));
                }
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando abbonamento: " + sim);
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<Map<String, Abbonamento>> getAllAbbonamenti() {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, Abbonamento> newEntries = new HashMap<>();
            selectAll("abbonamento", resultSet -> {
                final String sim = resultSet.getString("sim");
                final String tipoAbbonamento = resultSet.getString("abbonamento");
                final int messaggiRimanenti = resultSet.getInt("messaggi_rimanenti");
                final int chiamateRimanenti = resultSet.getInt("chiamate_rimanenti");
                final Abbonamento subscription = new Abbonamento(sim, tipoAbbonamento, messaggiRimanenti, chiamateRimanenti);
                newEntries.put(sim, subscription);
            });
            return newEntries;
        }, executor);
    }

    public CompletableFuture<Void> saveAllSubscriptions(final List<Abbonamento> abbonamenti) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO abbonamento (sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE messaggi_rimanenti = ?, chiamate_rimanenti = ?";

            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {

                for (final Abbonamento abbonamento : abbonamenti) {
                    statement.setString(1, abbonamento.getSim());
                    statement.setString(2, abbonamento.getAbbonamento());
                    statement.setInt(3, abbonamento.getMessages());
                    statement.setInt(4, abbonamento.getCalls());
                    statement.setInt(5, abbonamento.getMessages());
                    statement.setInt(6, abbonamento.getCalls());
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (final SQLException e) {
                Common.error(e, "Errore batch save abbonamenti");
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> updateSubscription(final Abbonamento subscription) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "UPDATE abbonamento SET messaggi_rimanenti = ?, chiamate_rimanenti = ? WHERE sim = ?";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, subscription.getMessages());
                statement.setInt(2, subscription.getCalls());
                statement.setString(3, subscription.getSim());
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore aggiornando abbonamento: " + subscription.getSim());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * SIM - Operazioni asincrone
     */

    public CompletableFuture<Void> saveSim(final String number) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO sim (numero_sim) VALUES (?)";

            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, number);
                final int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new SimNumberAlreadyExistsException("Il numero SIM " + number + " esiste già.");
                }
            } catch (final SQLException e) {
                throw new SimSaveException("Errore durante il salvataggio del numero SIM", e);
            }
        }, executor);
    }

    public CompletableFuture<List<String>> getAllSim() {
        return CompletableFuture.supplyAsync(() -> {
            final List<String> newEntries = new ArrayList<>();
            selectAll("sim", resultSet -> {
                final String sim = resultSet.getString(1);
                newEntries.add(sim);
            });
            return newEntries;
        }, executor);
    }

    /**
     * Contatti - Operazioni asincrone con batch
     */

    public CompletableFuture<Void> saveContatto(final Contatto contatto) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO contatto (sim, numero, nome, cognome) VALUES (?, ?, ?, ?)";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, contatto.getSim());
                statement.setString(2, contatto.getNumber());
                statement.setString(3, contatto.getName());
                statement.setString(4, contatto.getSurname());
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando contatto: " + contatto.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<Contatto>>> getAllContatti() {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, List<Contatto>> newEntries = new HashMap<>();
            selectAll("contatto", rs -> {
                final int id = rs.getInt(1);
                final String sim = rs.getString(2);
                final String number = rs.getString(3);
                final String name = rs.getString(4);
                final String surname = rs.getString(5);
                final Contatto contatto = new Contatto(id, sim, number, name, surname);

                final List<Contatto> contatti = newEntries.getOrDefault(sim, new ArrayList<>());
                contatti.add(contatto);
                newEntries.put(sim, contatti);
            });
            return newEntries;
        }, executor);
    }

    public CompletableFuture<List<Contatto>> getContattiBySim(final String sim) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = "SELECT * FROM contatto WHERE sim = ?";
            final List<Contatto> contatti = new ArrayList<>();
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, sim);
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    contatti.add(new Contatto(resultSet.getInt(1), resultSet.getString(2),
                                            resultSet.getString(3), resultSet.getString(4), resultSet.getString(5)));
                }
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando contatti per sim: " + sim);
            }
            return contatti;
        }, executor);
    }

    public CompletableFuture<Void> updateContatto(final Contatto contatto) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "UPDATE contatto SET nome = ?, cognome = ? WHERE id = ?";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, contatto.getName());
                statement.setString(2, contatto.getSurname());
                statement.setInt(3, contatto.getId());
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore aggiornando contatto: " + contatto.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> deleteContatto(final int id) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "DELETE FROM contatto WHERE id = ?";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore eliminando contatto: " + id);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * Cronologia Messaggi - Operazioni asincrone
     */

    public CompletableFuture<Void> saveMessaggio(final HistoryMessaggio messaggio) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_messaggi (sim, numero_mittente, messaggio, data_messaggio, persa) VALUES (?, ?, ?, ?, ?)";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, messaggio.getSim());
                statement.setString(2, messaggio.getNumber());
                statement.setString(3, messaggio.getMessage());
                statement.setString(4, messaggio.getDate());
                statement.setBoolean(5, messaggio.isLost());
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando messaggio: " + messaggio.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<HistoryMessaggio>>> getAllHistoryMessaggi(final String input) {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, List<HistoryMessaggio>> newEntries = new HashMap<>();
            final List<HistoryMessaggio> messaggi = new ArrayList<>();

            selectAll("cronologia_messaggi", (rs) -> {
                final String sim = rs.getString(2);
                final String number = rs.getString(3);
                final String message = rs.getString(4);
                final String date = rs.getString(5);
                final boolean isLost = rs.getBoolean(6);

                if (input.equals(sim)) {
                    messaggi.add(new HistoryMessaggio(sim, number, date, message, isLost, false));
                }
                if (input.equals(number)) {
                    messaggi.add(new HistoryMessaggio(number, sim, date, message, isLost, true));
                }
            });
            newEntries.put(input, messaggi);
            return newEntries;
        }, executor);
    }

    /**
     * Cronologia Chiamate - Operazioni asincrone
     */

    public CompletableFuture<Void> saveChiamata(final HistoryChiamata chiamata) {
        return CompletableFuture.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_chiamate (sim, numero_chiamato, data_chiamata, is_persa) VALUES (?, ?, ?, ?)";
            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, chiamata.getSim());
                statement.setString(2, chiamata.getNumber());
                statement.setString(3, chiamata.getDate());
                statement.setBoolean(4, chiamata.isLost());
                statement.executeUpdate();
            } catch (final SQLException e) {
                Common.error(e, "Errore salvando chiamata: " + chiamata.getId());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<HistoryChiamata>>> getAllHistoryChiamate(final String input) {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, List<HistoryChiamata>> newEntries = new HashMap<>();
            final List<HistoryChiamata> chiamate = new ArrayList<>();

            selectAll("cronologia_chiamate", (rs) -> {
                final String sim = rs.getString(2);
                final String number = rs.getString(3);
                final String date = rs.getString(4);
                final boolean isLost = rs.getBoolean(5);

                if (input.equals(sim)) {
                    chiamate.add(new HistoryChiamata(sim, number, date, isLost, false));
                }
                if (input.equals(number)) {
                    chiamate.add(new HistoryChiamata(number, sim, date, isLost, true));
                }
            });
            newEntries.put(input, chiamate);
            return newEntries;
        }, executor);
    }

    public CompletableFuture<List<HistoryChiamata>> getHistoryChiamateBySim(final String number) {
        return CompletableFuture.supplyAsync(() -> {
            final List<HistoryChiamata> chiamate = new ArrayList<>();
            final String sql = "SELECT * FROM cronologia_chiamate WHERE sim = ? OR numero_chiamato = ?";

            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, number);
                statement.setString(2, number);
                final ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    final String sim = rs.getString(2);
                    final String numberTarget = rs.getString(3);

                    if (sim.equalsIgnoreCase(number)) {
                        chiamate.add(new HistoryChiamata(sim, numberTarget, rs.getString(4), rs.getBoolean(5), false));
                    }
                    if (numberTarget.equalsIgnoreCase(number)) {
                        chiamate.add(new HistoryChiamata(numberTarget, sim, rs.getString(4), rs.getBoolean(5), true));
                    }
                }
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia chiamate: " + number);
            }
            return chiamate;
        }, executor);
    }

    public CompletableFuture<List<HistoryMessaggio>> getHistoryMessaggiBySim(final String number) {
        return CompletableFuture.supplyAsync(() -> {
            final List<HistoryMessaggio> messaggi = new ArrayList<>();
            final String sql = "SELECT * FROM cronologia_messaggi WHERE sim = ? OR numero_mittente = ?";

            try (final Connection conn = getConnection();
                 final PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, number);
                statement.setString(2, number);
                final ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    final String sim = rs.getString(2);
                    final String numeroMittente = rs.getString(3);
                    final String date = rs.getString(5);
                    final String message = rs.getString(4);
                    final boolean isLost = rs.getBoolean(6);

                    if (sim.equalsIgnoreCase(number)) {
                        messaggi.add(new HistoryMessaggio(sim, numeroMittente, date, message, isLost, false));
                    } else {
                        messaggi.add(new HistoryMessaggio(numeroMittente, sim, date, message, isLost, true));
                    }
                }
            } catch (final SQLException e) {
                Common.error(e, "Errore recuperando cronologia messaggi: " + number);
            }
            return messaggi;
        }, executor);
    }

    /* Cleanup per shutdown */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

