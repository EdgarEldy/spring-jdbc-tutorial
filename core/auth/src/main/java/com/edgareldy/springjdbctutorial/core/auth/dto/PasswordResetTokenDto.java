package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Business shape of a password reset token (the token field is the hash, never the raw value).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PasswordResetTokenDto {

    private Long id;

    private Long userId;

    private String token;

    private String type;

    private Instant expiryDate;

    public PasswordResetTokenDto() {
    }

    public PasswordResetTokenDto(Long id, Long userId, String token, String type, Instant expiryDate) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.type = type;
        this.expiryDate = expiryDate;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PasswordResetTokenDto that = (PasswordResetTokenDto) o;
        return Objects.equals(id, that.id)
                && Objects.equals(userId, that.userId)
                && Objects.equals(token, that.token)
                && Objects.equals(type, that.type)
                && Objects.equals(expiryDate, that.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, type, expiryDate);
    }

    @Override
    public String toString() {
        return "PasswordResetTokenDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", token=" + (token == null ? "null" : "****") +
                ", type=" + type +
                ", expiryDate=" + expiryDate +
                "}";
    }
}
