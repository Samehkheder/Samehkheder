package Vokabeln5Backend01;

public class Configs {
    /**
     * Kompilerangabe auf welchen Port der Webserver lauscht.
     */
    protected static int VOC_PORT = 5433;

    /**
     * Kompilerangabe unter welcher IP-Adresse die Datenbank zu finden ist.
     */
    protected static String DB_IP = "localhost";

    /**
     * Kompilerangabe für den Port zur IP-Adresse für die Datenbank.
     */
    protected static int DB_PORT = 5432;

    /**
     * Kompilerangabe für den Namen der Datenbank.
     */
    protected static String DB_NAME = "vokabeln5db01";

    /**
     * Kompilerangabe für den Namen der Tabelle der Users .
     */
    protected static String T_USERS = "users";

    /**
     * Kompilerangabe für den Namen der Tabelle der Vokabeln.
     */
    protected static String T_VOC = "voc";

    /**
     * Kompilerangabe für den Namen der Tabelle der fehlhaften Anmeldungen.
     */
    protected static String T_IP = "fehlerhafteanmeldung";

    /**
     * Kompilerangabe unter welchem User sich das Programm bei der Datenbank
     * anmeldet.
     */
    protected static String DB_USER_NAME = "postgres";

    /**
     * Kompilerangabe für das Passwort des SQL Users.
     */
    protected static String DB_USER_PASSWORT = "IvyInfo";

    /**
     * Kompilerangabe für das Secret-Key für JWT Tokens.
     */
    protected static String SECRET_KEY = "MY_SECRET_KEY!";

    // damit diese Klasse keine Instanzen von sich erstellen lässt. Somit
    // greift man unmittelbar auf die Felder der Klassen direkt Mit
    // Configs.T_VOC usw.
    private Configs() {}
 }
