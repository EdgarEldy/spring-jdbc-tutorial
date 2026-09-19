package com.edgareldy.springjdbctutorial.ws.security;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the RSA key pair of the JWT signer from PEM resources (private PKCS#8, public X.509). Any problem raises an IllegalStateException naming the key, so startup fails clearly.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Only java.security is used (KeyFactory + the standard key specs), no extra library. The locations are
// Spring resource URLs (file:, classpath:...) resolved by the ResourceLoader.
public final class JwtKeyLoader {

    private JwtKeyLoader() {
    }

    public static PrivateKey loadPrivateKey(ResourceLoader loader, String property, String location) {
        byte[] der = readPem(loader, property, location, "PRIVATE KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA private key for " + property + " (PKCS#8 expected)", e);
        }
    }

    public static PublicKey loadPublicKey(ResourceLoader loader, String property, String location) {
        byte[] der = readPem(loader, property, location, "PUBLIC KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA public key for " + property + " (X.509 expected)", e);
        }
    }

    private static byte[] readPem(ResourceLoader loader, String property, String location, String type) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Missing required property " + property);
        }
        Resource resource = loader.getResource(location);
        String pem;
        try (InputStream in = resource.getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the key of " + property + " at " + location, e);
        }
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        if (!pem.contains(begin) || !pem.contains(end)) {
            throw new IllegalStateException("The key of " + property + " must be a PEM '" + type + "' block");
        }
        String body = pem.substring(pem.indexOf(begin) + begin.length(), pem.indexOf(end)).replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The key of " + property + " is not valid Base64 PEM", e);
        }
    }
}
