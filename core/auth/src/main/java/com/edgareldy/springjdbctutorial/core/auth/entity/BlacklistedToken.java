package com.edgareldy.springjdbctutorial.core.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Row of the blacklisted_tokens table (revoked JWT). The token column holds the SHA-256 hex digest of the JWT, never the JWT.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "token")
    private String token;

    @Column(name = "jti")
    private String jti;

    @Column(name = "blacklisted_at")
    private Instant blacklistedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    public BlacklistedToken() {
    }

    public BlacklistedToken(Long id, Long userId, String token, String jti, Instant blacklistedAt, Instant createdAt, Instant expiresAt, Instant validatedAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.jti = jti;
        this.blacklistedAt = blacklistedAt;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.validatedAt = validatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlacklistedToken that = (BlacklistedToken) o;
        return Objects.equals(id, that.id)
                && Objects.equals(userId, that.userId)
                && Objects.equals(token, that.token)
                && Objects.equals(jti, that.jti)
                && Objects.equals(blacklistedAt, that.blacklistedAt)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(expiresAt, that.expiresAt)
                && Objects.equals(validatedAt, that.validatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, jti, blacklistedAt, createdAt, expiresAt, validatedAt);
    }

    @Override
    public String toString() {
        return "BlacklistedToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", token=" + (token == null ? "null" : "****") +
                ", jti=" + jti +
                ", blacklistedAt=" + blacklistedAt +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", validatedAt=" + validatedAt +
                "}";
    }
}
