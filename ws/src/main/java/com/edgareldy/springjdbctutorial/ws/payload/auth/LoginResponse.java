package com.edgareldy.springjdbctutorial.ws.payload.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of a successful login: the Bearer JWT (masked in toString), its type and its expiry.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class LoginResponse {

    private String token;

    private String tokenType;

    private Instant expiresAt;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType, Instant expiresAt) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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
        LoginResponse that = (LoginResponse) o;
        return Objects.equals(token, that.token)
                && Objects.equals(tokenType, that.tokenType)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, tokenType, expiresAt);
    }

    @Override
    public String toString() {
        return "LoginResponse{" + "token=" + (token == null ? "null" : "****") + ", tokenType=" + tokenType + ", expiresAt=" + expiresAt + "}";
    }
}
