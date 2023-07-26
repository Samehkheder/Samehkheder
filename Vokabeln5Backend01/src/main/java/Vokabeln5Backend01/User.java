package Vokabeln5Backend01;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author mengesser
 */
public class User {

    private String email;
    private String password;

    /**
     * Speichert die Mailadresse des Users und hasht und speichert das Passwort.
     *
     * @param email Die Email-Addresse des Users
     * @param clearPassword Das Klartext-Passwort des Users, wird als hash
     * gespeichert.
     */
    public User(String email, String clearPassword) {
        this.email = email;

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        this.password = bCryptPasswordEncoder.encode(clearPassword); //Passwort wird nur als hash gespeichert.
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
