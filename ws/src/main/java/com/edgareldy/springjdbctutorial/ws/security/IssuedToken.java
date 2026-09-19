package com.edgareldy.springjdbctutorial.ws.security;

import java.time.Instant;
import java.util.Objects;

/**
 * A freshly issued JWT with its issue and expiry instants.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class IssuedToken {

    private final String token;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public IssuedToken(String token, Instant issuedAt, Instant expiresAt) {
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
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
        IssuedToken that = (IssuedToken) o;
        return Objects.equals(token, that.token) && Objects.equals(issuedAt, that.issuedAt)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, issuedAt, expiresAt);
    }

    @Override
    public String toString() {
        return "IssuedToken{token=****, issuedAt=" + issuedAt + ", expiresAt=" + expiresAt + "}";
    }
}
