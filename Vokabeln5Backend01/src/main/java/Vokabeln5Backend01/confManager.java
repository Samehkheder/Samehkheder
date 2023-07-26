package Vokabeln5Backend01;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class confManager {

    /**
     * Analysiert die Kommandozeilenargumente und aktualisiert die
     * voreingestellten Konfigurationsoptionen.
     * @param args Die Kommandozeilenargumente.
     */

    // Da hat man 2 Möglichkeiten an Kommandozeilenargumente, lange und kurze.
    // Also entweder bspw. -c CONFIG_FILE oder --config CONFIG_FILE usw.
    // True/False, ob dieses Argument eine Option dahinter erwartet.
    // Letztendlich ist was dieses Argument wirkt.
    static protected void parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        // Config file
        options.addOption("c", "config", true,
            "Config file");
        // help manual
        options.addOption("h", "help", false,
            "Show this help, and exit");
        // sql server configs
        options.addOption("i", "db-ip", true,
            "IP of the sql server (default: localhost)");
        options.addOption("j", "db-port", true,
            "Port on which the sql server will listen (default: 5432)");
        // spark port
        options.addOption("l", "voc-port", true,
            "Port on which the web server will listen (default: 5433)");
        // sql user config
        options.addOption("n", "db-username", true,
            "SQL username (default: postgres)");
        options.addOption("p", "db-password", true,
            "SQL password (default: IvyInfo)");
        // User's secret key (JWT)
        options.addOption("s", "secret-key", true,
            "JWT secret key (default: 'MY_SECRET_KEY!')");
        // sql database configs
        options.addOption("t", "db-name", true,
            "SQL main target database (default: vokabeln5db01)");
        options.addOption("u", "t-users-name", true,
            "SQL users' table name (default: users)");
        options.addOption("v", "t-voc-name", true,
            "SQL vocabulary's table name (default: voc)");
        options.addOption("w", "t-voc-ip", true,
            "SQL users' IP table name (default: fehlerhafteanmeldung)");
        
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Voc6", options);
                System.exit(0);
            }
            if (cmd.hasOption("c")) {
                String fileName = cmd.getOptionValue("c");
                readConfigs(fileName);
            }
            if (cmd.hasOption("i")) {
                Configs.DB_IP = cmd.getOptionValue("i");
            }
            if (cmd.hasOption("j")) {
                Configs.DB_PORT = Integer.parseInt(cmd.getOptionValue("j"));
            }
            if (cmd.hasOption("l")) {
                Configs.VOC_PORT = Integer.parseInt(cmd.getOptionValue("l"));
            }
            if (cmd.hasOption("n")) {
                Configs.DB_USER_NAME = cmd.getOptionValue("n");
            }
            if (cmd.hasOption("p")) {
                Configs.DB_USER_PASSWORT = cmd.getOptionValue("p");
            }
            if (cmd.hasOption("s")) {
                Configs.SECRET_KEY = cmd.getOptionValue("s");
            }
            if (cmd.hasOption("t")) {
                Configs.DB_NAME = cmd.getOptionValue("t");
            }
            if (cmd.hasOption("u")) {
                Configs.T_USERS = cmd.getOptionValue("u");
            }
            if (cmd.hasOption("v")) {
                Configs.T_VOC = cmd.getOptionValue("v");
            }
            if (cmd.hasOption("w")) {
                Configs.T_IP = cmd.getOptionValue("w");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Liest die benutzerdefinierten Konfiguriereungen aus einer Datei ein.
     * Wenn die Datei nicht existiert, dann wird ein Fehler gezeigt.
     * Die Configs müssen in JSON Format geschrieben werden. Mögliche Keys sind
     * in der Klasse Configs zu finden.
     * @param fileName Der Name der Datei, aus dem Die Configs eingelesen
     * werden.
     */
    static protected void readConfigs(String fileName) {
        Gson gson = new GsonBuilder().serializeNulls().create();

        StringBuilder configs = new StringBuilder();
        try {
            File configFile = new File(fileName);
            Scanner readFile = new Scanner(configFile);
            while (readFile.hasNextLine()) {
                configs.append(readFile.nextLine());
            }
            readFile.close();
        } catch (FileNotFoundException e) {
            Logger.getLogger(confManager.class.getName())
                .log(Level.WARNING, null, e);
            System.err.println("Config file is not found.");
            return;
        }

        try {
            _Configs conf = gson.fromJson(configs.toString(), _Configs.class);
            // Null sind die String keys wenn die keinen definierten Wert haben.
            // Die Integer keys werden hingegen auf 0 initialisiert.
            if (conf.VOC_PORT != 0) {
                Configs.VOC_PORT = conf.VOC_PORT;
            }
            if (conf.DB_IP != null) {
                Configs.DB_IP = conf.DB_IP;
            }
            if (conf.DB_PORT != 0) {
                Configs.DB_PORT = conf.DB_PORT;
            }
            if (conf.DB_NAME != null) {
                Configs.DB_NAME = conf.DB_NAME;
            }
            if (conf.T_USERS != null) {
                Configs.T_USERS = conf.T_USERS;
            }
            if (conf.T_VOC != null) {
                Configs.T_VOC = conf.T_VOC;
            }
            if (conf.T_IP != null) {
                Configs.T_IP = conf.T_IP;
            }
            if (conf.DB_USER_NAME != null) {
                Configs.DB_USER_NAME = conf.DB_USER_NAME;
            }
            if (conf.DB_USER_PASSWORT != null) {
                Configs.DB_USER_PASSWORT = conf.DB_USER_PASSWORT;
            }
            if (conf.SECRET_KEY != null) {
                Configs.SECRET_KEY = conf.SECRET_KEY;
            }
        } catch (JsonParseException e) {
            Logger.getLogger(confManager.class.getName())
                .log(Level.WARNING, null, e);
            System.err.println("Key values in '" + fileName + "' are not valid. Consult the help menu.");
        }
    }

    // Eine Hilfsklasse für GSON, da man keine Instanz von Configs
    // erstellen kann.
    private static class _Configs {
        private int VOC_PORT;
        private String DB_IP;
        private int DB_PORT;
        private String DB_NAME;
        private String T_USERS;
        private String T_VOC;
        private String T_IP;
        private String DB_USER_NAME;
        private String DB_USER_PASSWORT;
        private String SECRET_KEY;
    }
}
