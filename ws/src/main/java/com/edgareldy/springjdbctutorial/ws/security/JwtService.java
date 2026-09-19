package com.edgareldy.springjdbctutorial.ws.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies the RS256 JWTs of the API. parse never throws: any invalid token gives an empty Optional.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// A JWT (JSON Web Token) is a signed, self-contained bearer credential: header.claims.signature. The
// server keeps no session, it just verifies the signature and reads the claims (here the user id, email
// and permissions), which is what makes the API stateless.
//
// RS256 (RSA + SHA-256) is a deliberate choice over HS256: it is asymmetric, so only the holder of the
// PRIVATE key can issue tokens while verification needs only the PUBLIC key. A service that merely
// verifies tokens never has to hold a signing secret. Keys are read once, at construction.
public class JwtService {

    private static final String PERMISSIONS_CLAIM = "permissions";
    private static final String EMAIL_CLAIM = "email";
    private static final String ALGORITHM = "RS256";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Duration validity;
    private final String issuer;
    private final Clock clock;

    public JwtService(ResourceLoader loader, String privateKeyProperty, String privateKeyLocation,
                      String publicKeyProperty, String publicKeyLocation, long expirationSeconds,
                      String issuer, Clock clock) {
        this.privateKey = JwtKeyLoader.loadPrivateKey(loader, privateKeyProperty, privateKeyLocation);
        this.publicKey = JwtKeyLoader.loadPublicKey(loader, publicKeyProperty, publicKeyLocation);
        this.validity = Duration.ofSeconds(expirationSeconds);
        this.issuer = issuer;
        this.clock = clock;
    }

    public IssuedToken issue(com.edgareldy.springjdbctutorial.core.auth.dto.UserDto user) {
        // JWT times have second precision, so truncate to keep the returned instants equal to the claims
        Instant issuedAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(validity);
        List<String> permissions = user.getPermissions() == null ? List.of() : user.getPermissions();
        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(PERMISSIONS_CLAIM, permissions)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedToken(token, issuedAt, expiresAt);
    }

    public Optional<ParsedToken> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token);
            // verifyWith(RSA key) would also accept RS384/RS512: only RS256 is issued, so only RS256 passes
            if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                return Optional.empty();
            }
            Claims claims = jws.getPayload();
            Object rawPermissions = claims.get(PERMISSIONS_CLAIM);
            List<String> permissions = rawPermissions instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : List.of();
            if (claims.getSubject() == null || claims.getId() == null
                    || claims.getIssuedAt() == null || claims.getExpiration() == null) {
                return Optional.empty();
            }
            return Optional.of(new ParsedToken(Long.valueOf(claims.getSubject()), claims.get(EMAIL_CLAIM, String.class),
                    claims.getId(), claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(),
                    permissions));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** SHA-256 hex of the raw JWT: the only form in which a token is ever stored or looked up. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
