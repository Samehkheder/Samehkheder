package Vokabeln5Backend01;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import spark.Filter;
import spark.Request;
import spark.Spark;

/**
 * @author mengesser
 */
public class UserManager {

    /**
     * Lambda-Funktion, braucht im req-Body ein JSON-Objekt eines Users.
     * Antwortet je nach Erfolg mit entsprechenden HTML-Codes.
     */
    static Route registerCall = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        String json = req.body();
        User u = isValidJsonUser(json);
        if (u != null) { // Prüfen, ob der request-Body ein gültiger Json-String ist
            if (!isValidEmail(u.getEmail())) {
                res.status(400);
                return "Email is not valid!";
            }
            if (u.getPassword().length() == 0) {
                res.status(400);
                return "Password can't be blank!";
            }
            if (!register(json)) { // Benutzer wird versucht zu registrieren
                res.status(500);
                return "email already in use";
            }
            res.status(200);
            return "Registered";
        } else {
            res.status(400);
            return "Not valid JSON";
        }
    };

    /**
     * Eine Funktion zu überprüfen, ob die E-Mail eine gültige ist oder nicht
     */
    protected static boolean isValidEmail(String email) {
        String ac = "[a-zA-Z0-9._]{1,32}"; // allowed chars
        String regex = "^" + ac + "@" + ac + "\\." + ac + "$";
        if (Pattern.compile(regex).matcher(email).matches()) {
            return true;
        }
        return false;
    }

    /**
     * Filter zur Kontrolle der Anmeldung. Die Funktion Kontrolliert ob die
     * Header email und password für eine korrekte Anmeldung richtig gesetzt
     * sind. Ist dies nicht der Fall, wird der Request mit 401 Abgebrochen.
     */
    protected static Filter loginFilter = (request, response) -> {
        if(Crypto.JWTCheck(request, response)) {
            return;
        }
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Content-Type", "text/plain");
        if (!loginHeadersMissing(request)) {
            if (checkValidLogin(request)) {
                response.status(200);
                return;
            } else {
                documentFehlerhafteAnmeldung(request);
//                System.err.println("Falsche Credetials ");
                Spark.halt(401, response.body() + "\nE-mail oder Passwort falsch");
            }
        } else {
            Spark.halt(401, response.body() + "\nE-Mail und/oder Passwort Header fehlen");
//        System.err.println("Fehlerhafte Anmeldung");
        }
    };

    /**
     * Filter, ob die IP-Adresse gesperrt ist.
     * <p>Es wird die Tabelle fehlerhafteanmeldung bereinigt, sodass keine 
     * Datensätze vorhanden sind, die älter als eine Stunde sind, da ältere 
     * Daten irrelevant sind. Danach kann geprüft werden, ob die IP-Adresse noch 
     * gesperrt ist. Zudem wird in der <code>request</code> gespeichert wie 
     * viele fehlerhafte Anmeldeversuche die IP-Adresse schon vorzuweisen hat.</p>
     * <p>Zudem wird die Benutzung des Dienstes Verwehrt, wenn sich keine 
     * Verbindung zu Datenbank aufbauen lässt. Das hat den hintergrund, dass 
     * zum einen die Nachfolgenden Dienste nicht ausgeführt werden können, und 
     * zum anderen der passwortBruteForceSchutz nicht überprüft werden konnte. 
     * Hier heißt es "im Zweifel lieber den Zugriff verweigern".</p>
     */
    protected static Filter passwortBruteForceSchutz = (request, response) -> {
        Connection conn = Startup.initVerbindungDatenbank();
        if(conn == null) {
            Spark.halt(503, "Keine Verbindung zur Datenbank");
        }
        try (Statement statement = conn.createStatement()) {
            String q = String.format("DELETE FROM %s WHERE zeitpunkt<(localtimestamp - interval '1 hour')", Configs.T_IP);
            statement.execute(q);
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, "Brute Force Schutz Löschen alter Datensätze Fehler", e);
            System.err.println("Fehler beim Löschen der alten falsche Anmeldung Daten!");
        }
        try {
            String q = String.format("SELECT COUNT(*), " + 
                "MAX(gesperrtbis)>localtimestamp FROM %s " + 
                "WHERE ip=?::INET", Configs.T_IP);
            PreparedStatement preparedStatement = conn.prepareStatement(q);
            preparedStatement.setString(1, request.ip());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            if (resultSet.getBoolean(2) == true) {
                Spark.halt(401, "SIE SIND VORRÜBERGEHEND GESPERRT!");
                return;
            }
            request.attribute("fehlerhafteanmeldung", resultSet.getInt(1));
        } catch (SQLException e) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, "Brute Force Schutz GESPERRT-Abfrage Fehler", e);
            System.err.println("Fehler bei ist IP gesperrt? Abfrage!");
        }
    };

    /**
     * Documentiert eine Fehlerhafte Anmeldung.
     * <p>Wenn eine IP-Adresse falsche Anmeldedaten (Passwort, Email, JWT) 
     * angibt, wird dies in der Tabelle fehlerhafteanmeldung mit dem 
     * dazugehörigen Zeitpunkt gespeichert. Wenn der Nutzer mit diesem 
     * Methodenaufruf und den bisherigen fehlerhaften Anmeldungen insgesamt 
     * mehr als 5 Fehlerhafte Anmeldungen vorzuweisen hat, wird er zudem für 
     * die nächste halbe Stunde als gesperrt markiert. Dafür wird in der 
     * Datenbank der Zeitpunkt "gesperrt bis" eingetragen.</p>
     * @param request Das Request-Objekt des Threads mit <code>attribut("fehlerhafteanmeldung")</code>
     */
    protected static void documentFehlerhafteAnmeldung(Request request) {
        Connection conn = Startup.initVerbindungDatenbank();
        String q = String.format("INSERT INTO %s VALUES (?::INET, localtimestamp", Configs.T_IP);
        System.err.println((int) request.attribute("fehlerhafteanmeldung"));
        if((int) request.attribute("fehlerhafteanmeldung") + 1 > 5) {//Mehr als 5 fehlversuche (Zu den bisherigen in request.attribute kommt der, der diese Methode aufgerufen hat.)
            q += ", localtimestamp + interval '30 minutes')";
        } else {
            q += ")";
        }
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(q);
            preparedStatement.setString(1, request.ip());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, "Brute Force Schutz documentFehlerhafteAnmeldung Fehler", e);
            System.err.println("Fehler beim documentieren einer fehlerhaften Anmeldung!");
        }
    }

    /**
     * Prüft ob der Nutzer sich Korrekt anmeldet. Sucht in der Datenbank nach
     * einem Eintrag mit email und password wie im übergebenen request header.
     * Zudem wird, sofern der Nutzer sein korektes passwort eingegeben hat, in
     * der request seine user_id al Attribut gespeichert.
     *
     * @param request mit nicht leeren Headern email und password
     * @return true, falls der Nutzer sich korrekt angemeldet hat, sonst false
     */
    protected static boolean checkValidLogin(Request request) {
        Connection conn = Startup.initVerbindungDatenbank();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String q = String.format("SELECT password, id FROM %s WHERE email = ?", Configs.T_USERS);
        try {
            PreparedStatement statement = conn.prepareStatement(q);
            statement.setString(1, request.headers("email"));
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            if (passwordEncoder.matches(request.headers("password"), resultSet.getString(1))) {
                request.attribute("user_id", resultSet.getInt("id"));
                return true;
            } else {
//                System.err.println("Passwort Mismatch");
            }
        } catch (SQLException e) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, "Passwordabfrage Fehler", e);
            System.err.println("Fehler beim Abrufen des Passworts!");
        }
        return false;

    }

    /**
     * Kontrolliert, ob die Header email und password vorhanden sind.
     *
     * @param request Die request aus der die Header ausgelesen werden sollen.
     * @return true Falls mindestens ein Header fehlt.
     */
    protected static boolean loginHeadersMissing(Request request) {
        if (request.headers("email") == null || request.headers("password") == null) {
            return true;
        }
        return false;
    }

    /**
     * Registriert einen neuen Benutzer in der Datenbank
     *
     * @param jsonString JSON-String, der einen Benutzer darstellt
     * @return true, falls der Nutzer zur Datenbank hinzugefügt wurde, false,
     * falls die Email bereits in Verwendung ist
     */
    private static boolean register(String jsonString) {
        Gson gson = new Gson();
        User user = new User(gson.fromJson(jsonString, User.class).getEmail(), gson.fromJson(jsonString, User.class).getPassword()); // User-Objekt aus json-String extrahieren, dabei wird das Passwort gehasht gespeichert
        if (isUniqueEmail(user.getEmail())) { // Prüfen ob Mail einzigartig ist
            Connection conn = Startup.initVerbindungDatenbank();
            String q = String.format("INSERT INTO %s (email, password) VALUES (?, ?)", Configs.T_USERS);
            try {
                PreparedStatement statement = conn.prepareStatement(q); // PreparedStatement wird verwendet, um Injection vorzubeugen
                statement.setString(1, user.getEmail());
                System.out.println(user.getPassword());
                statement.setString(2, user.getPassword());
                statement.executeUpdate();
            } catch (SQLException ex) {
                Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Fehler bei Einfügen in Datenbank.");
            }
        } else { // Wenn nein, direkt return
            return false;
        }
        return true;
    }

    /**
     * Prüft, ob eine Email-Addresse noch nicht in der Datenbank vorhanden ist
     *
     * @param checkMail Email-Addresse die auf Einzigartigkeit geprüft werden
     *                  soll
     * @return true wenn die Addresse einzigartig ist, false wenn nicht
     */
    private static boolean isUniqueEmail(String checkMail) {
        Connection conn = Startup.initVerbindungDatenbank(); // Verbindung zur Datenbank herstellen
        String q = String.format("SELECT COUNT(*) FROM users WHERE email=?", Configs.T_USERS);
        try {
            PreparedStatement st = conn.prepareStatement(q);
            st.setString(1, checkMail);
            ResultSet rs = st.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Fehler bei Datenbankabfrage.");
            return false; // Bei Fehler sicherheitshalber auch false zurückgeben
        }
    }

    /**
     * Prüft, ob ein gegebener String ein gültiges JSON von einem User-Objekt
     * darstellt
     *
     * @param jsonString Der zu prüfende Json-String
     * @return Instanz der Klasse User wenn String ein JSON-Objekt eines Users ist,
     * null wenn nicht
     */
    public static User isValidJsonUser(String jsonString) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(jsonString, User.class);
        } catch (com.google.gson.JsonSyntaxException ex) { // Wenn Anfrage kein JSON enthält dann Rückgabe false
            return null;
        }
    }

    /**
     * ändert die Benutzer-Email nachdem es geprüft wurde, ob die Email und das Passwort richtig sind.
     * es Wird dann die Email vom Body() ausgelesen und in der DB geändert. Sonst, wird es eine Fehlermeldung zurückgegeben.
     *
     */
    static Route changeEmail = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        //gibt aus dem Request Body() das JsonObjekt zurück und davon das JsonElement mit dem key "newEmail", falls es vorhanden ist
        JsonObject jsonObject = JsonParser.parseString(req.body()).getAsJsonObject();
        JsonElement myElement = jsonObject.get("newEmail");
        if (myElement == null) {
            res.status(400);
            return "Error!";
        }
        // löscht die ""-Zeichen vom json value Element.
        String myStringElement = myElement.toString().replaceAll("\"", "");

        Connection conn = Startup.initVerbindungDatenbank();
        try (Statement statement = conn.createStatement()) {

            //Alternativ
            //PreparedStatement statement = conn.prepareStatement("SEELECT COUNT(*) FROM users WHERE email= ? ;"); // PreparedStatement wird verwendet, um Injection vorzubeugen
            //statement.setString(1, myStringElement);
            String q = String.format("SELECT DISTINCT email FROM %s", Configs.T_USERS);
            try (ResultSet result = statement.executeQuery(q)) {
                while (result.next()) {
                    String email = result.getString("email");
                    if (email.equalsIgnoreCase(myStringElement)) {
                        res.status(400);
                        return "Email is already used!";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            String q = String.format("UPDATE %s SET email = ? WHERE id = ? ;", Configs.T_USERS);
            PreparedStatement statement = conn.prepareStatement(q); // PreparedStatement wird verwendet, um Injection vorzubeugen
            statement.setString(1, myStringElement);
            statement.setInt(2, ((int) req.attribute("user_id")));
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Fehler bei Einfügen in Datenbank.");
        }
        System.out.println(myStringElement);
        res.status(200);
        return "Email changed successfully!";
    };


    /**
     * ändert die Benutzer-Passwort nachdem es geprüft wurde, ob die Email und das Passwort richtig sind.
     * es Wird dann das Passwort vom Body() ausgelesen und in der DB geändert, nachdem es verschlüsselt wird.
     * Sonst, wird es eine Fehlermeldung zurückgegeben.
     *
     */
    static Route changePassword = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        //gibt aus dem Request Body() das JsonObjekt zurück und davon das JsonElement mit dem key "newEmail", falls es vorhanden ist
        JsonObject jsonObject = JsonParser.parseString(req.body()).getAsJsonObject();
        JsonElement myElement = jsonObject.get("newPassword");
        if (myElement == null) {
            res.status(400);
            return "Error!";
        }
        // löscht die ""-Zeichen vom json value Element.
        String newPassword = myElement.toString().replaceAll("\"", "");
        // Passwort wird verschlüsselt
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        newPassword = bCryptPasswordEncoder.encode(newPassword);

//        String userName = req.headers("email");
        Connection conn = Startup.initVerbindungDatenbank();
        try {
            String q = String.format("UPDATE %s SET password = ? WHERE id=?", Configs.T_USERS);
            PreparedStatement statement = conn.prepareStatement(q); // PreparedStatement wird verwendet, um Injection vorzubeugen
            statement.setString(1, newPassword);
            statement.setInt(2, req.attribute("user_id"));
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Fehler bei Einfügen in Datenbank.");
        }
        res.status(200);
        return "Password changed successfully!";
    };
}
