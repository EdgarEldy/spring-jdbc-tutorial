package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Input of a logout: the SHA-256 hash of the presented JWT, its jti and its validity window.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class BlacklistedTokenDto {

    private Long userId;

    private String tokenHash;

    private String jti;

    private Instant issuedAt;

    private Instant expiresAt;

    public BlacklistedTokenDto() {
    }

    public BlacklistedTokenDto(Long userId, String tokenHash, String jti, Instant issuedAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.jti = jti;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlacklistedTokenDto that = (BlacklistedTokenDto) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(tokenHash, that.tokenHash)
                && Objects.equals(jti, that.jti)
                && Objects.equals(issuedAt, that.issuedAt)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tokenHash, jti, issuedAt, expiresAt);
    }

    @Override
    public String toString() {
        return "BlacklistedTokenDto{" +
                "userId=" + userId +
                ", tokenHash=" + (tokenHash == null ? "null" : "****") +
                ", jti=" + jti +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                "}";
    }
}
