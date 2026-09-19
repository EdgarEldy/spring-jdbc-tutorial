package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Business shape of an activation token (the token field is the hash, never the raw value).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ActivationTokenDto {

    private Long id;

    private Long userId;

    private String token;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant validatedAt;

    public ActivationTokenDto() {
    }

    public ActivationTokenDto(Long id, Long userId, String token, Instant createdAt, Instant expiresAt, Instant validatedAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
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
        ActivationTokenDto that = (ActivationTokenDto) o;
        return Objects.equals(id, that.id)
                && Objects.equals(userId, that.userId)
                && Objects.equals(token, that.token)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(expiresAt, that.expiresAt)
                && Objects.equals(validatedAt, that.validatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, createdAt, expiresAt, validatedAt);
    }

    @Override
    public String toString() {
        return "ActivationTokenDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", token=" + (token == null ? "null" : "****") +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", validatedAt=" + validatedAt +
                "}";
    }
}
