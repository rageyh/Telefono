package me.zrageyh.telefono.manager;

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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public final class Database extends SimpleDatabase {
    @Getter
    private static final Database instance = new Database();
    @Getter
    private int pingTicks;

    private Database() {
        pingTicks = 0;
    }


    @Override
    protected void onConnected() {
        long now = System.currentTimeMillis();

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

    private void updatePing(long oldTime) {
        pingTicks = (int) MathUtil.ceiling((double) (System.currentTimeMillis() - oldTime) / 1000.0 * 50.0 * 1.3);
        if (System.currentTimeMillis() - oldTime > 250L) {
            Common.warning("La connessione al database è lenta (" + MathUtil.formatTwoDigits((double) (System.currentTimeMillis() - oldTime) / 1000.0) + " secondi). Cerca di ridurre il carico e usare il database in localhost.");
            return;
        }
        Common.log("Database MySQL creato in: " + pingTicks + " secondi");
    }


    /**
     * ABBONAMENTO
     */


    public void saveSubscription(Abbonamento subscription) {

        Common.runAsync(() -> {
            final String sql = "INSERT INTO abbonamento (sim, abbonamento, messaggi_rimanenti, chiamate_rimanenti) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE abbonamento = ?, messaggi_rimanenti = ?, chiamate_rimanenti = ?";

            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, subscription.getSim());
                statement.setString(2, subscription.getAbbonamento());
                statement.setInt(3, subscription.getMessages());
                statement.setInt(4, subscription.getCalls());
                statement.setString(5, subscription.getAbbonamento());
                statement.setInt(6, subscription.getMessages());
                statement.setInt(7, subscription.getCalls());
                statement.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException(e);

            }
        });
    }


    public Optional<Abbonamento> getSubscription(String sim) {
        Valid.checkAsync("Il caricamento dei dati deve essere async");


        final String sql = "SELECT * FROM abbonamento WHERE sim = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, sim);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(new Abbonamento(sim, rs.getString(2), rs.getInt(3), rs.getInt(4)));
            }
        } catch (SQLException e) {
            Common.error(e, "Errore durante il recupero del record utente da MySQL. Restituzione di dati incompleti...");
        }
        return Optional.empty();


    }


    public Map<String, Abbonamento> getAllAbbonamenti() {
        Valid.checkAsync("Il caricamento dei dati deve essere async");
        Map<String, Abbonamento> newEntries = new HashMap<>();

        selectAll("abbonamento", resultSet -> {
            String sim = resultSet.getString("sim");
            String tipoAbbonamento = resultSet.getString("abbonamento");
            int messaggiRimanenti = resultSet.getInt("messaggi_rimanenti");
            int chiamateRimanenti = resultSet.getInt("chiamate_rimanenti");
            Abbonamento subscription = new Abbonamento(sim, tipoAbbonamento, messaggiRimanenti, chiamateRimanenti);
            newEntries.put(sim, subscription);
        });

        return newEntries;
    }

    public void saveAllSubscriptions(List<Abbonamento> abbonamenti) {
        for (Abbonamento abbonamento : abbonamenti) {
            updateSubscription(abbonamento);
        }
    }

    public void updateSubscription(Abbonamento subscription) {

        final String sql = "UPDATE abbonamento SET messaggi_rimanenti = ?, chiamate_rimanenti = ? WHERE sim = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, subscription.getMessages());
            statement.setInt(2, subscription.getCalls());
            statement.setString(3, subscription.getSim());
            statement.executeUpdate();
        } catch (SQLException e) {
            Common.error(e, "Errore durante l'aggiornamento dell'abbonamento %s. Abbonamento non aggiornato...".formatted(subscription.getSim()));
        }
    }


    /**
     * SIM
     */


    public CompletableFuture<Void> saveSim(String number) {
        Valid.checkAsync("Salvare i dati deve essere async");
        final String sql = "INSERT INTO sim (numero_sim) VALUES (?)";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, number);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SimNumberAlreadyExistsException("Il numero SIM " + number + " esiste già.");
            }
        } catch (SQLException e) {
            throw new SimSaveException("Errore durante il salvataggio del numero SIM", e);
        }

        return CompletableFuture.completedFuture(null);
    }


    public List<String> getAllSim() {
        Valid.checkAsync("Il caricamento dei dati deve essere async");

        List<String> newEntries = new ArrayList<>();

        selectAll("sim", resultSet -> {
            String sim = resultSet.getString(1);
            newEntries.add(sim);
        });
        return newEntries;
    }


    /**
     * Contatti
     */


    public void saveContatto(Contatto contatto) {
        Common.runAsync(() -> {
            final String sql = "INSERT INTO contatto (sim, numero, nome, cognome) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, contatto.getSim());
                statement.setString(2, contatto.getNumber());
                statement.setString(3, contatto.getName());
                statement.setString(4, contatto.getSurname());
                statement.executeUpdate();
            } catch (SQLException e) {
                Common.error(e, "Errore durante il salvataggio del contatto con id: %d. Contatto non salvato...".formatted(contatto.getId()));
            }
        });
    }

    public Map<String, List<Contatto>> getAllContatti() {

        Valid.checkAsync("Il caricamento dei dati deve essere async");

        Map<String, List<Contatto>> newEntries = new HashMap<>();
        selectAll("contatto", rs -> {
            int id = rs.getInt(1);
            String sim = rs.getString(2);
            String number = rs.getString(3);
            String name = rs.getString(4);
            String surname = rs.getString(5);
            Contatto contatto = new Contatto(id, sim, number, name, surname);

            List<Contatto> contatti = newEntries.getOrDefault(sim, new ArrayList<>());
            contatti.add(contatto);
            newEntries.put(sim, contatti);
        });

        return newEntries;
    }

    public List<Contatto> getContattiBySim(String sim) {
        Valid.checkAsync("Il salvataggio dei dati deve essere async");

        final String sql = "SELECT * FROM contatto WHERE sim = ?";
        List<Contatto> contatti = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, sim);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                contatti.add(new Contatto(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
            }
        } catch (SQLException e) {
            Common.error(e, "Errore durante il recupero del record utente da MySQL. Restituzione di dati incompleti...");
        }
        return contatti;
    }


    public void updateContatto(Contatto contatto) {
        Common.runAsync(() -> {
            final String sql = "UPDATE contatto SET nome = ?, cognome = ? WHERE id = ?";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, contatto.getName());
                statement.setString(2, contatto.getSurname());
                statement.setInt(3, contatto.getId());
                statement.executeUpdate();
            } catch (SQLException e) {
                Common.error(e, "Errore durante l'aggiornamento del contatto con id: %d. Contatto non aggiornato...".formatted(contatto.getId()));
            }
        });
    }


    public void deleteContatto(int id) {
        Common.runAsync(() -> {
            final String sql = "DELETE FROM contatto WHERE id = ?";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                Common.error(e, "Errore durante l'eliminazione del contatto con id: %d. Contatto non eliminato...".formatted(id));
            }
        });
    }


    /**
     * Cronologia Messaggi
     */

    public void saveMessaggio(HistoryMessaggio messaggio) {

        Common.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_messaggi (sim, numero_mittente, messaggio, data_messaggio, persa) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, messaggio.getSim());
                statement.setString(2, messaggio.getNumber());
                statement.setString(3, messaggio.getMessage());
                statement.setString(4, messaggio.getDate());
                statement.setBoolean(5, messaggio.isLost());
                statement.executeUpdate();
            } catch (SQLException e) {
                Common.error(e, "Errore durante il salvataggio del messaggio con id: %d. Messaggio non salvato...".formatted(messaggio.getId()));
            }
        });
    }


    public Map<String, List<HistoryMessaggio>> getAllHistoryMessaggi(String input) {
        Valid.checkAsync("Il caricamento dei dati deve essere async");
        Map<String, List<HistoryMessaggio>> newEntries = new HashMap();
        List<HistoryMessaggio> chiamate = newEntries.getOrDefault(input, new ArrayList<>());

        selectAll("cronologia_messaggi", (rs) -> {
            String sim = rs.getString(2);
            String number = rs.getString(3);
            String message = rs.getString(4);
            String date = rs.getString(5);
            boolean isLost = rs.getBoolean(6);
            HistoryMessaggio chiamata;
            if (input.equals(sim)) {
                chiamata = new HistoryMessaggio(sim, number, date, message, isLost, false);
                chiamate.add(chiamata);
            }

            if (input.equals(number)) {
                chiamata = new HistoryMessaggio(number, sim, date, message, isLost, true);
                chiamate.add(chiamata);
            }

        });
        newEntries.put(input, chiamate);
        return newEntries;
    }


    /**
     * Cronologia Chiamate
     */

    public void saveChiamata(HistoryChiamata chiamata) {
        Common.runAsync(() -> {
            final String sql = "INSERT INTO cronologia_chiamate (sim, numero_chiamato, data_chiamata, is_persa) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, chiamata.getSim());
                statement.setString(2, chiamata.getNumber());
                statement.setString(3, chiamata.getDate());
                statement.setBoolean(4, chiamata.isLost());
                statement.executeUpdate();
            } catch (SQLException e) {
                Common.error(e, "Errore durante il salvataggio della chiamata con id: %d. Chiamata non salvata...".formatted(chiamata.getId()));
            }
        });
    }


    public Map<String, List<HistoryChiamata>> getAllHistoryChiamate(String input) {
        Valid.checkAsync("Il caricamento dei dati deve essere async");
        Map<String, List<HistoryChiamata>> newEntries = new HashMap();
        List<HistoryChiamata> chiamate = newEntries.getOrDefault(input, new ArrayList<>());

        selectAll("cronologia_chiamate", (rs) -> {
            String sim = rs.getString(2);
            String number = rs.getString(3);
            String date = rs.getString(4);
            boolean isLost = rs.getBoolean(5);
            HistoryChiamata chiamata;
            if (input.equals(sim)) {
                chiamata = new HistoryChiamata(sim, number, date, isLost, false);
                chiamate.add(chiamata);
            }

            if (input.equals(number)) {
                chiamata = new HistoryChiamata(number, sim, date, isLost, true);
                chiamate.add(chiamata);
            }

        });
        newEntries.put(input, chiamate);
        return newEntries;
    }

    public List<HistoryChiamata> getHistoryChiamateBySim(String number) {
        Valid.checkAsync("Il caricamento dei dati deve essere async");
        List<HistoryChiamata> chiamate = new ArrayList<>();

        final String sql = "SELECT * FROM cronologia_chiamate WHERE sim = ? OR numero_chiamato = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, number);
            statement.setString(2, number);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String sim = rs.getString(2);
                String numberTarget = rs.getString(3);

                if (sim.equalsIgnoreCase(number)) {
                    chiamate.add(new HistoryChiamata(sim, number, rs.getString(4), rs.getBoolean(5), rs.getBoolean(6)));
                }

                if (numberTarget.equalsIgnoreCase(number)) {
                    chiamate.add(new HistoryChiamata(number, sim, rs.getString(4), rs.getBoolean(5), rs.getBoolean(6)));
                }
            }
        } catch (SQLException e) {
            Common.error(e, "Errore durante il recupero del record utente da MySQL. Restituzione di dati incompleti...");
        }
        return chiamate;

    }

    public List<HistoryMessaggio> getHistoryMessaggiBySim(String number) {
        Valid.checkAsync("Il caricamento dei dati deve essere async");
        List<HistoryMessaggio> messaggi = new ArrayList<>();

        final String sql = "SELECT * FROM cronologia_messaggi WHERE sim = ? OR numero = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, number);
            statement.setString(2, number);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String sim = rs.getString(2);
                String date = rs.getString(4);
                String message = rs.getString(5);
                boolean isLost = rs.getBoolean(6);
                if (sim.equalsIgnoreCase(number)) {
                    messaggi.add(new HistoryMessaggio(sim, number, date, message, isLost, false));
                } else {
                    messaggi.add(new HistoryMessaggio(number, sim, date, message, isLost, true));
                }
            }
        } catch (SQLException e) {
            Common.error(e, "Errore durante il recupero del record utente da MySQL. Restituzione di dati incompleti...");
        }
        return messaggi;
    }


}

