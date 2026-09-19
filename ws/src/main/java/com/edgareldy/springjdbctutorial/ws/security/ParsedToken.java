package com.edgareldy.springjdbctutorial.ws.security;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Verified content of a JWT: user id, email, jti, validity window and permission codes.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ParsedToken {

    private final Long userId;
    private final String email;
    private final String jti;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final List<String> permissions;

    public ParsedToken(Long userId, String email, String jti, Instant issuedAt, Instant expiresAt,
                       List<String> permissions) {
        this.userId = userId;
        this.email = email;
        this.jti = jti;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.permissions = permissions;
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

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParsedToken that = (ParsedToken) o;
        return Objects.equals(userId, that.userId) && Objects.equals(email, that.email)
                && Objects.equals(jti, that.jti) && Objects.equals(issuedAt, that.issuedAt)
                && Objects.equals(expiresAt, that.expiresAt) && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, jti, issuedAt, expiresAt, permissions);
    }

    @Override
    public String toString() {
        return "ParsedToken{userId=" + userId + ", email=" + email + ", jti=" + jti + ", issuedAt=" + issuedAt
                + ", expiresAt=" + expiresAt + ", permissions=" + permissions + "}";
    }
}
