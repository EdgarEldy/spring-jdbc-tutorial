package com.edgareldy.springjdbctutorial.core.auth.service;

import com.edgareldy.springjdbctutorial.core.auth.dto.BlacklistedTokenDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;

/**
 * Registration, activation, login, logout, profile and password reset. Dto types at the boundary.
 * Authentication failures throw AuthenticationFailedException subclasses, business refusals BusinessRuleException.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface AuthService {

    /** Creates a disabled account and its activation token (raw token logged at INFO, no mailer). */
    UserDto register(UserDto user);

    void activateAccount(String rawToken);

    /** Returns the user with roles and permissions filled and a null password. */
    UserDto login(String email, String rawPassword);

    /** Revokes a JWT by storing its hash. Idempotent. */
    void logout(BlacklistedTokenDto token);

    boolean isTokenBlacklisted(String tokenHash);

    UserDto getProfile(Long userId);

    /** Never reveals whether the email exists. */
    void forgotPassword(String email);

    void resetPassword(String rawToken, String newPassword);

    /** Deletes the blacklisted rows that already expired and returns how many were removed. */
    int purgeExpiredTokens();
}
