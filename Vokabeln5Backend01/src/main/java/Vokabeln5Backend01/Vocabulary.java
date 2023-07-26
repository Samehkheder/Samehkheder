package Vokabeln5Backend01;

/**
 * @author
 */
    
public class Vocabulary {
    protected int id;
    protected String frage;
    protected String antwort;
    protected String sprache;
    protected int phase;
    protected String group;

    /**
     * Erstellt neue Vokabeln
     *
     * @param id Der von der Tabelle automatische generierte Schlüssel
     * @param frage Die Frage auf Deutsch
     * @param antwort Die Antwort auf fremder Sprache
     * @param sprache Die Sprache der Antwort
     * @param phase Die Phase. Beantwortet der Nutzer die Frage mit richtiger
     * Antwort, wird die Phase der Vokabel um eins erhöht, sonst um eins erniedrigt.
     * @param group Die Gruppe der diese Vokabel angehört.
     */
    protected Vocabulary(int id, String frage, String antwort, String sprache, int phase, String group) {
        this.id = id;
        this.frage = frage;
        this.antwort = antwort;
        this.sprache = sprache;
        this.phase = phase;
        this.group = group;
    }
}
