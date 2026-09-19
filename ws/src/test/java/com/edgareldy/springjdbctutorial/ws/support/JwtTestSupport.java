package com.edgareldy.springjdbctutorial.ws.support;

import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Test helpers around JWTs: a JwtService built on the development key pair (the locations surefire passes as
 * system properties), throw-away RSA keys, PEM encoding and forged tokens (foreign key, HS256, alg none...).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class JwtTestSupport {

    /** Issuer configured by default in SecurityConfig. */
    public static final String ISSUER = "spring-jdbc-tutorial";

    private JwtTestSupport() {
    }

    /** A JwtService on the dev keys, with the given clock and a one hour validity. */
    public static JwtService devJwtService(Clock clock) {
        return new JwtService(new DefaultResourceLoader(),
                "app.jwt.private-key-location", System.getProperty("app.jwt.private-key-location"),
                "app.jwt.public-key-location", System.getProperty("app.jwt.public-key-location"),
                3600L, ISSUER, clock);
    }

    /** Every permission of the V2 seed, in "RESOURCE:ACTION" form. */
    public static final List<String> ALL_PERMISSIONS = List.of(
            "USER:READ", "USER:WRITE", "ROLE:READ", "ROLE:WRITE", "PERMISSION:READ", "PERMISSION:WRITE",
            "CATEGORY:READ", "CATEGORY:WRITE", "PRODUCT:READ", "PRODUCT:WRITE", "CUSTOMER:READ", "CUSTOMER:WRITE",
            "ORDER:READ", "ORDER:WRITE");

    /**
     * A genuine token signed with the dev private key, carrying exactly the given permissions in its claim.
     * The controller tests use it instead of a login: the mocked service layer has no database to log into.
     */
    public static String tokenWithPermissions(long userId, List<String> permissions) {
        com.edgareldy.springjdbctutorial.core.auth.dto.UserDto user = new com.edgareldy.springjdbctutorial.core.auth.dto.UserDto(
                userId, "Test", "User", "user-" + userId + "@example.com", null, true, false, List.of(), permissions);
        return devJwtService(Clock.systemUTC()).issue(user).getToken();
    }

    /** A fresh 2048 bit RSA key pair, never related to the dev keys. */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Wraps DER bytes into a PEM block of the given type (for example "PRIVATE KEY"). */
    public static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }

    public static String privatePem(PrivateKey key) {
        return pem("PRIVATE KEY", key.getEncoded());
    }

    public static String publicPem(PublicKey key) {
        return pem("PUBLIC KEY", key.getEncoded());
    }

    /** A well-formed RS256 token (issuer, jti, iat, exp, email, permissions) signed with the given private key. */
    public static String rs256Token(PrivateKey key, long userId, Instant issuedAt, Instant expiresAt) {
        return baseBuilder(userId, issuedAt, expiresAt).signWith(key, Jwts.SIG.RS256).compact();
    }

    /** Same claims, signed with RS384: a valid signature that the service must still refuse. */
    public static String rs384Token(PrivateKey key, long userId, Instant issuedAt, Instant expiresAt) {
        return baseBuilder(userId, issuedAt, expiresAt).signWith(key, Jwts.SIG.RS384).compact();
    }

    /** Algorithm confusion attack: an HS256 token whose HMAC secret is the (public) RSA public key bytes. */
    public static String hs256TokenSignedWithPublicKey(PublicKey publicKey, long userId, Instant issuedAt,
                                                       Instant expiresAt) {
        SecretKey secret = Keys.hmacShaKeyFor(publicKey.getEncoded());
        return baseBuilder(userId, issuedAt, expiresAt).signWith(secret, Jwts.SIG.HS256).compact();
    }

    /** An unsecured JWT (header alg none, no signature). */
    public static String unsignedToken(long userId, Instant issuedAt, Instant expiresAt) {
        return baseBuilder(userId, issuedAt, expiresAt).compact();
    }

    /** A signed token with a different issuer. */
    public static String rs256TokenWithIssuer(PrivateKey key, String issuer, long userId, Instant issuedAt,
                                              Instant expiresAt) {
        return Jwts.builder().subject(String.valueOf(userId)).id(UUID.randomUUID().toString()).issuer(issuer)
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .claim("email", "forged@example.com").claim("permissions", List.of())
                .signWith(key, Jwts.SIG.RS256).compact();
    }

    /** A signed token that has no jti claim. */
    public static String rs256TokenWithoutJti(PrivateKey key, long userId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder().subject(String.valueOf(userId)).issuer(ISSUER)
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .claim("email", "forged@example.com").claim("permissions", List.of())
                .signWith(key, Jwts.SIG.RS256).compact();
    }

    private static io.jsonwebtoken.JwtBuilder baseBuilder(long userId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder().subject(String.valueOf(userId)).id(UUID.randomUUID().toString()).issuer(ISSUER)
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .claim("email", "forged@example.com").claim("permissions", List.of("USER:READ"));
    }
}
