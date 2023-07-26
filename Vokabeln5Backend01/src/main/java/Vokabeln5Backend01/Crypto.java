package Vokabeln5Backend01;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import io.jsonwebtoken.*;

import java.sql.*;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import spark.Request;
import spark.Response;
import spark.Route;

public class Crypto {

    /**
     * Drei Monaten Laufzeit für Token Datum-Ablauf
     */
    private final static long THREE_MONTH_INTERVAL = 7776000000L;//1000 * 60 * 60 * 24 * 90
    //private final static long THREE_MONTH_INTERVAL = 1000*60;// Test token expired in one minute
    /**
     * zur Authentifizierung eines neuen JWT Tokens
     */
    static Route JWTRegister = (req, res) -> {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        String id = null, issuer = null, subject = null;
        /*
        JsonArray jsonObjects;
        try {
            jsonObjects = JsonParser.parseString(req.body()).getAsJsonArray();
        } catch (Exception e) {
            return "No JSON Object Found!";
        }
        if (!checkJWTValid(jsonObjects)) {
            return "Invalid Json Object";
        }

        if (!checkIfJsonElementMissing(jsonObjects, 0, "alg")) {
            alg = jsonObjects.get(0).getAsJsonObject().get("alg").toString().replaceAll("\"", "");
        }
        if (!checkIfJsonElementMissing(jsonObjects, 1, "jti")) {
            id = jsonObjects.get(1).getAsJsonObject().get("jti").toString().replaceAll("\"", "");
        }
        if (!checkIfJsonElementMissing(jsonObjects, 1, "iss")) {
            issuer = jsonObjects.get(1).getAsJsonObject().get("iss").toString().replaceAll("\"", "");
        }
        if (!checkIfJsonElementMissing(jsonObjects, 1, "sub")) {
            subject = jsonObjects.get(1).getAsJsonObject().get("sub").toString().replaceAll("\"", "");
        }

        if (!idIsValid(id)) {
            Spark.halt(401, "Email Invalid!"); 
        }
        */
        id = "" + req.attribute("user_id");
        issuer = "Vokabeln 5 Backend Server";
        subject = "Dein Login JWT für den Vokabeln 5 Backend Server";
        return "Bearer:" + Crypto.createJWT(id, issuer, subject);
    };

    /**
     * checken ob AuthorizationsHeader fehlt
     *
     * @param request Standard Request
     * @return True falls fehlt, sonst False
     */
    protected static boolean authorizationHeaderMissing(Request request) {
        if (request.headers("Token") == null) {
            return true;
        }
        return false;
    }

//    /**
//     * checken ob JWT Objekt gültig
//     *
//     * @param arr json Array von req.body()
//     * @return true falls gültig, sonst False
//     */
//    protected static boolean checkJWTValid(JsonArray arr) {
//        if (arr.size() > 1) {
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * checken ob jsonElement vorhanden ist
//     *
//     * @param arr             jsonArray
//     * @param index           Index Nummer des JsonObject von req.body()
//     * @param jsonElementName jsonElemnt Name
//     * @return true falls fehlt, sonst false
//     */
//    protected static boolean checkIfJsonElementMissing(JsonArray arr, int index, String jsonElementName) {
//        if (arr.get(index).getAsJsonObject().get(jsonElementName) == null) {
//            return true;
//        }
//        return false;
//    }

    /**
     * zur valedierung eines Bearers
     */
    protected static boolean JWTCheck(Request req, Response res) {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Content-Type", "text/plain");
        if (authorizationHeaderMissing(req)) {
//            Spark.halt(401, "No Authorization Header found");
            res.body("No Authorization Header found");
            return false;
        }
        String authority = req.headers("Token");
        if (!authority.toLowerCase().startsWith("bearer")) {
//            Spark.halt(401, "No Bearer were found");
            res.body("No Bearer were found");
            return false;
        }
        Claims infos = decodeJWT(authority.substring(7));
        if (infos == null) {
//            Spark.halt(401, "Token Invalid!");
            res.body("Token Invalid!");
            UserManager.documentFehlerhafteAnmeldung(req);
            return false;
        }
        if (tokenIsExpired(infos)) {
//            Spark.halt(401, "Token Expired");
            res.body("Token Expired");
            UserManager.documentFehlerhafteAnmeldung(req);
            return false;
        }
        try {
            req.attribute("user_id", Integer.parseInt(infos.getId()));
        } catch (NumberFormatException e) {
            res.body("Token Invalid!"); //Die user_id Fehlt, also ist der Token Falsch
            UserManager.documentFehlerhafteAnmeldung(req);
            return false;
        }
        res.status(200);
        return true;
    };

    /**
     * zur Generierung eines JWT Tokens
     *
     * @param alg     zur Verschlüsselung benutzten Algorithmus
     * @param id      User_id des Users, wird aus dem Login übernommen
     * @param issuer  iss
     * @param subject sub
     * @return String JWT Token
     * @throws SQLException
     */
    public static String createJWT(String id, String issuer, String subject) throws SQLException {

        SignatureAlgorithm signatureAlgorithm;

        /*
        //für bel. Algo.
        switch (alg.toUpperCase()) {
            case "HS256":
                signatureAlgorithm = SignatureAlgorithm.HS256;
                break;
            case "ES384":
                signatureAlgorithm = SignatureAlgorithm.ES384;
                break;
            case "RS256":
                signatureAlgorithm = SignatureAlgorithm.RS256;
                break;
            default:
                signatureAlgorithm = SignatureAlgorithm.NONE;
        }

        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            System.out.println("Keine Algo. Typ bekommen");
            return null;
        }
        */
        
        signatureAlgorithm = SignatureAlgorithm.HS256;
        
        //Kodierungsschritt
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Configs.SECRET_KEY);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Buildingsschritt
        JwtBuilder builder = Jwts.builder().setId(id)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(signatureAlgorithm, signingKey);

        //Ablaufdatum
        long expMillis = System.currentTimeMillis() + THREE_MONTH_INTERVAL;
        Date expDate = new Date(expMillis);

        builder.setExpiration(expDate);
        //decodeJWT(builder.compact());
        //addToJwt(builder.compact(), getPass(id));
        return builder.compact();
    }

    /**
     * checken ob token abgelaufen ist
     * @param token der jwt token
     * @return true falls abgelaufen, sonst false
     */
    private static boolean tokenIsExpired(Claims token) {
        return token.getExpiration().before(new Date());// <
    }


    /**
     * Dekodierungsschritt zur Validierung von JWT Token
     * @param jwt JWT Token
     * @return JWT Token dekodiert
     */
    public static Claims decodeJWT(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(Configs.SECRET_KEY))
                    .parseClaimsJws(jwt).getBody();
            System.out.println("here: " + claims);
            return claims;
        } catch (Exception e) {
            return null;
        }

    }

   /* private static String getPass(String usr) {
        System.out.println(usr);
        usr = "'" + usr + "'";
        Connection conn = Startup.initVerbindungDatenbank();
        try (Statement statement = conn.createStatement()) {
            String emails = "SELECT * FROM users where email like " + usr + ";";
            try (ResultSet result = statement.executeQuery(emails)) {
                while (result.next()) {
                    String pass = result.getString("password");
                    return pass;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }*/

//    /**
//     * checken ob email gueltig ist
//     * @param id die Email
//     * @return true falls gueltig, sonst false
//     */
//    private static boolean idIsValid(String id) {
//        if (!id.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
//            return false;
//        }
//        return true;
//    }

    /*
    private static boolean addToJwt(String jwt, String secretKey) {
        Connection conn = Startup.initVerbindungDatenbank();
        try {
            PreparedStatement statement = conn.prepareStatement("INSERT INTO  jwt (bearer, secretKey) VALUES (?, ?)"); // PreparedStatement wird verwendet, um Injection vorzubeugen
            statement.setString(1, jwt);
            statement.setString(2, secretKey);
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Fehler bei Einfügen in Datenbank.");
            return false;
        }
        return true;
    }

    private static String getFromJwt(String jwt){
        jwt = "'" + jwt + "'";
        Connection conn = Startup.initVerbindungDatenbank();
        try (Statement statement = conn.createStatement()) {
            String secrets = "SELECT * FROM jwt where bearer like " + jwt + ";";
            try (ResultSet result = statement.executeQuery(secrets)) {
                while (result.next()) {
                    String pass = result.getString("secretKey");
                    return pass;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }*/

}
