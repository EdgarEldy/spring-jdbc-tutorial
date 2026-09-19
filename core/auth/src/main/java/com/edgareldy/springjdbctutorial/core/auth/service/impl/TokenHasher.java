package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates random raw tokens and computes the SHA-256 hex digest that is the only form stored.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
final class TokenHasher {

    private static final int TOKEN_BYTES = 32;

    // Instance field on purpose (never static): one generator per service instance, seeded by the OS.
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 32 random bytes, URL-safe base64 without padding (43 characters).
     */
    String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Why SHA-256 and not bcrypt for tokens: a token is 256 bits of randomness, so it cannot be
    // brute-forced or found in a dictionary and needs no slow, salted hash. It must also be looked up
    // BY VALUE (WHERE token = ?), which a per-hash random salt (bcrypt) makes impossible. A database
    // leak still yields only digests, which cannot be replayed as a token. Passwords are the opposite
    // case (low entropy, chosen by humans), hence bcrypt for those.
    /**
     * SHA-256 of the UTF-8 bytes of the value, as 64 lowercase hex characters.
     */
    String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every Java platform implementation
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
