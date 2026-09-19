package com.edgareldy.springjdbctutorial.ws.security;

import java.time.Instant;
import java.util.Objects;

/**
 * Principal placed in the SecurityContext for an authenticated request: who, and which token (hash, jti, validity) so logout can revoke it.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final String jti;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public AuthenticatedUser(Long userId, String email, String jti, String tokenHash, Instant issuedAt,
                             Instant expiresAt) {
        this.userId = userId;
        this.email = email;
        this.jti = jti;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getJti() {
        return jti;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticatedUser that = (AuthenticatedUser) o;
        return Objects.equals(userId, that.userId) && Objects.equals(email, that.email)
                && Objects.equals(jti, that.jti) && Objects.equals(tokenHash, that.tokenHash)
                && Objects.equals(issuedAt, that.issuedAt) && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, jti, tokenHash, issuedAt, expiresAt);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{userId=" + userId + ", email=" + email + ", jti=" + jti
                + ", tokenHash=****, issuedAt=" + issuedAt + ", expiresAt=" + expiresAt + "}";
    }
}
