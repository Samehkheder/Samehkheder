package Vokabeln5Backend01;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import spark.Request;
import spark.Route;

public class vocManager {
    private static Gson gson = new GsonBuilder()
        .setPrettyPrinting().serializeNulls().create();
    private static ResultSet rs;

    /**
     * Lambda-Funktion
     * Antwortet je nach Erfolg mit allen Vokabeln eines Users.
     */
    protected static Route getAll = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        String q = String.format("SELECT id, frage, antwort, sprache, phase, gruppe FROM "
            + "%s WHERE user_id = ?", Configs.T_VOC);
        try (Connection conn = Startup.initVerbindungDatenbank()) {
            PreparedStatement st = conn.prepareStatement(q);
            st.setInt(1, req.attribute("user_id"));
            ResultSet rs = st.executeQuery();
            return toJson(rs);
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                    .log(Level.SEVERE, "Retrieving vocabulary failed!", e);
            res.status(500);
            return e.toString();
        }
    };

    /**
     * Lambda-Funktion
     * Antwortet je nach Erfolg mit einer zufälligen Vokabel.
     */
    protected static Route getRandom = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        String q = String.format("SELECT id, frage, antwort, sprache, phase, gruppe FROM %s "
            + "WHERE phase != 5 AND user_id = ? ORDER BY random() LIMIT 1", Configs.T_VOC);
        try (Connection conn = Startup.initVerbindungDatenbank()) {
            PreparedStatement st = conn.prepareStatement(q);
            st.setInt(1, req.attribute("user_id"));
            ResultSet rs = st.executeQuery();
            return toJson(rs);
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                    .log(Level.SEVERE, "Retrieving vocabulary failed!", e);
            res.status(500);
            return e.toString();
        }
    };

    /**
     * Lambda-Funktion, braucht im req-Body ein JSON Objekt einer Vokabel. 
     * Das Objekt muss jeweils eine Frage, Antwort und Sprache beinhalten. Das 
     * Feld "Phase" ist optional und wird automatisch auf 1 initialisiert. Das 
     * Feld "Group" ist optional und wird automatisch auf "no group" initialisiert.
     * Antwortet je nach Erfolg mit "Done".
     */
    protected static Route addVoc = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        Vocabulary voc = fromJson(req);
        if (voc.phase == 0) {
            voc.phase = 1;
        }
        if (voc.group == null) {
            voc.group = "no group";
        }
        String q = String.format("INSERT INTO %s (user_id, frage, antwort, "
            + "sprache, phase, gruppe) VALUES (?, ?, ?, ?, ?, ?)", Configs.T_VOC);
        try (Connection conn = Startup.initVerbindungDatenbank()) {
            PreparedStatement st = conn.prepareStatement(q);
            st.setInt(1, req.attribute("user_id"));
            st.setString(2, voc.frage);
            st.setString(3, voc.antwort);
            st.setString(4, voc.sprache);
            st.setInt(5, voc.phase);
            st.setString(6, voc.group);
            st.execute();
            return "Done";
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                    .log(Level.SEVERE, "Inserting vocabulary failed!", e);
            res.status(500);
            return e.toString();
        }
    };

    /**
     * Lambda-Funktion, braucht im req-Body ein JSON Objekt einer Vokabel. 
     * Alle Felder im Objekt sind
     * optional, und nur die gegebenen werden die in der Datenbank
     * überschrieben.
     * Antwortet je nach Erfolg mit "Done".
     */
    protected static Route updateVoc = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        int id = gson.fromJson(req.splat()[0], int.class);
        Vocabulary voc = fromJson(req);
        String q = String.format("SELECT * FROM %s WHERE id=? AND user_id =?",
            Configs.T_VOC);
        try (Connection conn = Startup.initVerbindungDatenbank()) {
            // TYPE_SCROLL_INSENSITIVE:
            // unempfindlich gegenüber Änderungen, die durch Abfragen begangen wurden
            // CONCUR_UPDATABLE:
            // Aktualisierbar, um die Zeile mit einer Abfrage zu ändern
            PreparedStatement st = conn.prepareStatement(q,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            st.setInt(1, id);
            st.setInt(2, req.attribute("user_id"));
            rs = st.executeQuery();
            while (rs.next()) {
                if (voc.frage != null)
                    rs.updateString("frage", voc.frage);
                if (voc.antwort != null)
                    rs.updateString("antwort", voc.antwort);
                if (voc.sprache != null)
                    rs.updateString("sprache", voc.sprache);
                if (voc.phase != 0)
                    rs.updateInt("phase", voc.phase);
                if (voc.group != null)
                    rs.updateString("gruppe", voc.group);
                rs.updateRow();
            }
            return "Done";
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                    .log(Level.SEVERE, "Updating vocabulary failed!", e);
            res.status(500);
            return e.toString();
        }
    };

    /**
     * Lambda-Funktion.
     * Die Vokabel mit dem ID: ID wird gelöscht.
     * Antwortet je nach Erfolg mit "Done".
     */
    protected static Route deleteVoc = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        int id = gson.fromJson(req.splat()[0], int.class);
        String q = String.format("DELETE FROM %s WHERE id=? AND user_ID=?",
            Configs.T_VOC);
        try (Connection conn = Startup.initVerbindungDatenbank()) {
            PreparedStatement st = conn.prepareStatement(q);
            st.setInt(1, id);
            st.setInt(2, req.attribute("user_id"));
            st.execute();
            return "Done";
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                    .log(Level.SEVERE, "Deleting vocabulary failed!", e);
            res.status(500);
            return e.toString();
        }
    };

    /**
     * Eine Vokabel wird pro Zeile erstellt und in ein Vector gespeichert.
     * Dann wird der ganze Vektor in JSON umgewandelt.
     * @param rs Die ResultSet, aus der die Zeilen der Tabelle ausgelesen und in
     * JSON umgewandelt werden.
     * @return JSON String von der Abfrage.
     */
    static String toJson(ResultSet rs) {
        Vector<Vocabulary> list = new Vector<>();
        try {
            while (rs.next()) {
                int id = rs.getInt(1);
                String frage = rs.getString(2);
                String antwort = rs.getString(3);
                String sprache = rs.getString(4);
                int phase = rs.getInt(5);
                String group = rs.getString(6);
                Vocabulary v = new Vocabulary(id, frage, antwort, sprache, phase, group);
                list.add(v);
            }
            return gson.toJson(list);
        } catch (SQLException e) {
            Logger.getLogger(vocManager.class.getName())
                .log(Level.SEVERE, "Parsing vocabulary to JSON failed!", e);
            return e.toString();
        }
    }

    /**
     * Prüft ob das vom Nutzer übergebene Objekt im Body eine gültige Vokabel ist
     * und gibt ein Objekt vom Typ Vocabulary zurück. Falls einige Felder nicht im
     * Body sind, werden sie mit NULL für Strings und 0 für Integers initialisiert.
     * @param req Die Request, aus deren Body das JSON Objekt einer Vokabel
     * ausgelesen werden soll.
     * @return eine initialisierte Instanz von Vocabulary.
     */
    static Vocabulary fromJson(Request req) {
        String r = req.body();
        // OPTIONAL:
        // In der Tabelle sind die Felder (frage, antwort, sprache) alle NOT NULL,
        // und die Felder in SQL sind case-sensitive, deswegen werden diese Felder
        // in der Request mit den kleingeschriebenen Alternativen ersetzt.
        r = r.replaceAll("(?i)\"frage\":", "\"frage\":");
        r = r.replaceAll("(?i)\"antwort\":", "\"antwort\":");
        r = r.replaceAll("(?i)\"sprache\":", "\"sprache\":");
        r = r.replaceAll("(?i)\"phase\":", "\"phase\":");
        r = r.replaceAll("(?i)\"group\":", "\"group\":");
        try {
            return gson.fromJson(r, Vocabulary.class);
        } catch (JsonParseException e) {
            Logger.getLogger(vocManager.class.getName())
                .log(Level.SEVERE, "Parsing vocabulary from JSON failed!", e);
            return null;
        }
    }
}
