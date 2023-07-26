package Vokabeln5Backend01;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import spark.ModelAndView;
import spark.Response;
import spark.template.velocity.VelocityTemplateEngine;
import static spark.Spark.*;

public class Startup {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        confManager.parse(args);
        staticFiles.location("/www");
        startServer();
        Connection test = initVerbindungDatenbank();
        before(UserManager.passwortBruteForceSchutz);
        post("/register", UserManager.registerCall); // Bei post-Anfragen auf /register wird mit UserManager.registerCall ein neuer User erstellt
        before("/login", UserManager.loginFilter);
        get("/login", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Content-Type", "text/plain");
            return "logged in";
        });
        before("/voc", UserManager.loginFilter);
        //before("/voc", Crypto.JWTCheck);
        path("/voc", () -> {
            before("*",     UserManager.loginFilter);
            get("",         vocManager.getAll);
            post("",        vocManager.addVoc);
            
            before("/*",    UserManager.loginFilter);
            get("/random",  vocManager.getRandom);
            patch("/*",     vocManager.updateVoc);
            delete("/*",    vocManager.deleteVoc);
        });
        get("/app/register", (req, res) -> render("www/register.html", res));
        notFound((req, res) -> render("www/404.html", res));
        
        
        before("/changeEmail", UserManager.loginFilter);
        //before("/changeEmail", Crypto.JWTCheck);
        post("/changeEmail", UserManager.changeEmail);

        before("/login/jwt", UserManager.loginFilter);
        get("/login/jwt", Crypto.JWTRegister);
        //get("/JWTCheck", Crypto.JWTCheck);

        before("/changePassword", UserManager.loginFilter);
        //before("/changePassword", Crypto.JWTCheck);
        post("/changePassword", UserManager.changePassword);

        /*
        before("/hello", Crypto.JWTCheck);
        get("/hello", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Content-Type", "text/plain");
            return "Hello from server";
        });
        */
    }

    protected static String render(String path, Response res) {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/html; charset=UTF-8");
        HashMap<String, Object> model = new HashMap<>();
        return new VelocityTemplateEngine().render(
            new ModelAndView(model, path));
    }

    /**
     * Starten des embedded Jetty Webservers.
     * <p>
     * Staret mithilfe des Spark-Frameworks einen embedded
     * <a href="http://eclipse.org/jetty/">Jetty</a> Webserver auf dem Localhost
     * und unter Port <code>VOC_PORT</code>. Sollte das starten Scheitern, wird
     * die Fehlermeldung in der Konsole ausgegeben und das Programm beendet.</p>
     */
    protected static void startServer() {
        initExceptionHandler(e -> {
            System.out.println("Uh-oh");
            System.err.println(e);
            System.out.println("Starten des Servers gescheitert.");
            System.exit(100);
        });
        port(Configs.VOC_PORT);
        init();
    }

    /**
     * Baue Vervindung zur Datenbank auf. Meldet sich als User
     * <code>DB_USER_NAME</code> mit Passwort <code>DB_USER_PASSWORT</code> bei
     * der Datenbank an, und gibt die hergesstellte Verbindung
     * <code>Connection</code> zurück.
     *
     * @return Verbindung zur DB
     */
    protected static Connection initVerbindungDatenbank() {
        String url = String.format("jdbc:postgresql://%s:%d/%s", Configs.DB_IP, Configs.DB_PORT, Configs.DB_NAME);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, Configs.DB_USER_NAME, Configs.DB_USER_PASSWORT);
            System.out.println("Verbindung zur DB aufgebaut.");
        } catch (SQLException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Verbindung konnte nicht aufgebaut werden.");
        }
        return conn;
    }
}
