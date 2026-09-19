package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.dto.BlacklistedTokenDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.ActivationToken;
import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;
import com.edgareldy.springjdbctutorial.core.auth.entity.PasswordResetToken;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.exception.AccountNotActiveException;
import com.edgareldy.springjdbctutorial.core.auth.exception.InvalidCredentialsException;
import com.edgareldy.springjdbctutorial.core.auth.mapper.UserMapper;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Implementation of AuthService. Declared by @Bean in ServiceConfig (no stereotype) and returned as
 * the interface so the @Transactional advice applies through a JDK proxy.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuthServiceImpl implements AuthService {

    private static final Log LOG = LogFactory.getLog(AuthServiceImpl.class);

    private static final int MAX_PASSWORD_BYTES = 72;
    private static final Duration ACTIVATION_VALIDITY = Duration.ofHours(24);
    private static final Duration RESET_VALIDITY = Duration.ofHours(1);
    private static final String RESET_TYPE = "PASSWORD_RESET";

    private final UserDao userDao;
    private final ActivationTokenDao activationTokenDao;
    private final PasswordResetTokenDao passwordResetTokenDao;
    private final BlacklistedTokenDao blacklistedTokenDao;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final UserMapper userMapper = new UserMapper();
    private final TokenHasher tokenHasher = new TokenHasher();
    // Verified against when the email is unknown, so a failed login costs one bcrypt check either way
    private final String dummyHash;

    public AuthServiceImpl(UserDao userDao, ActivationTokenDao activationTokenDao,
                           PasswordResetTokenDao passwordResetTokenDao, BlacklistedTokenDao blacklistedTokenDao,
                           PasswordEncoder passwordEncoder, Clock clock) {
        this.userDao = userDao;
        this.activationTokenDao = activationTokenDao;
        this.passwordResetTokenDao = passwordResetTokenDao;
        this.blacklistedTokenDao = blacklistedTokenDao;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-dummy-password");
    }

    // @Transactional (enabled by @EnableTransactionManagement in DataSourceConfig) makes the Spring proxy
    // open a transaction before the method and commit it on return, or roll back on a RuntimeException.
    // Every write below therefore succeeds or fails as one unit (e.g. user row + activation token).
    @Override
    @Transactional
    public UserDto register(UserDto input) {
        if (input == null) {
            throw new BusinessRuleException("User is required");
        }
        String email = normalizeEmail(input.getEmail());
        requirePasswordWithinLimit(input.getPassword());
        if (userDao.existsByEmail(email)) {
            throw new BusinessRuleException("Email is already registered");
        }
        UserDto toCreate = new UserDto(null, input.getFirstName(), input.getLastName(), email, null,
                false, false, null, null);
        User created;
        try {
            created = userDao.insert(userMapper.toEntity(toCreate, passwordEncoder.encode(input.getPassword())));
        } catch (DuplicateKeyException e) {
            // Lost a race against a concurrent registration of the same email: the unique index decided
            throw new BusinessRuleException("Email is already registered", e);
        }
        String rawToken = tokenHasher.generateRawToken();
        Instant now = clock.instant();
        activationTokenDao.insert(new ActivationToken(null, created.getId(), tokenHasher.sha256Hex(rawToken),
                now, now.plus(ACTIVATION_VALIDITY), null));
        // No mailer exists in this tutorial: the raw token is only ever visible in this log line
        LOG.info("Activation token for " + email + ": " + rawToken);
        return userMapper.toDto(created);
    }

    @Override
    @Transactional
    public void activateAccount(String rawToken) {
        Instant now = clock.instant();
        ActivationToken token = rawToken == null ? null
                : activationTokenDao.findByTokenHash(tokenHasher.sha256Hex(rawToken)).orElse(null);
        if (token == null || token.getValidatedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("Invalid or expired activation token");
        }
        userDao.updateEnabled(token.getUserId(), true);
        activationTokenDao.markValidated(token.getId(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto login(String email, String rawPassword) {
        User user = email == null ? null : userDao.findByEmail(normalizeEmail(email)).orElse(null);
        boolean passwordOk;
        if (user == null) {
            // Fake verification: same bcrypt cost as a real one, result ignored
            passwordEncoder.matches("x", dummyHash);
            passwordOk = false;
        } else if (rawPassword == null || exceedsLimit(rawPassword)) {
            // Over 72 bytes can never match a bcrypt hash: plain invalid credentials, never a 500
            passwordEncoder.matches("x", dummyHash);
            passwordOk = false;
        } else {
            passwordOk = passwordEncoder.matches(rawPassword, user.getPassword());
        }
        if (!passwordOk) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled() || user.isAccountLocked()) {
            throw new AccountNotActiveException();
        }
        return withRolesAndPermissions(user);
    }

    @Override
    @Transactional
    public void logout(BlacklistedTokenDto token) {
        if (blacklistedTokenDao.existsByTokenHash(token.getTokenHash())) {
            return;
        }
        Instant now = clock.instant();
        try {
            blacklistedTokenDao.insert(new BlacklistedToken(null, token.getUserId(), token.getTokenHash(),
                    token.getJti(), now, token.getIssuedAt() == null ? now : token.getIssuedAt(),
                    token.getExpiresAt(), null));
        } catch (DuplicateKeyException e) {
            // Concurrent or repeated logout of the same token (same hash or jti): already revoked, accept
            LOG.debug("Token already blacklisted, logout accepted");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String tokenHash) {
        return blacklistedTokenDao.existsByTokenHash(tokenHash);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return withRolesAndPermissions(user);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        // Token and hash are generated whether or not the account exists, so the work is alike
        String rawToken = tokenHasher.generateRawToken();
        String tokenHash = tokenHasher.sha256Hex(rawToken);
        // A null or blank email is simply an unknown account here: normalizeEmail would throw a 422 and
        // reveal a different behaviour than the generic answer
        String normalized = email == null || email.isBlank() ? "" : normalizeEmail(email);
        User user = userDao.findByEmail(normalized).orElse(null);
        // The delete runs for an unknown account too (on an id that matches nothing), so the only work
        // skipped when the email is unknown is the final insert and the log line
        passwordResetTokenDao.deleteByUserId(user == null ? -1L : user.getId());
        if (user == null) {
            return;
        }
        passwordResetTokenDao.insert(new PasswordResetToken(null, user.getId(), tokenHash, RESET_TYPE,
                clock.instant().plus(RESET_VALIDITY)));
        LOG.info("Password reset token for " + normalized + ": " + rawToken);
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = rawToken == null ? null
                : passwordResetTokenDao.findByTokenHash(tokenHasher.sha256Hex(rawToken)).orElse(null);
        if (token == null || !token.getExpiryDate().isAfter(clock.instant())) {
            throw new BusinessRuleException("Invalid or expired reset token");
        }
        requirePasswordWithinLimit(newPassword);
        userDao.updatePassword(token.getUserId(), passwordEncoder.encode(newPassword));
        passwordResetTokenDao.deleteByUserId(token.getUserId());
    }

    @Override
    @Transactional
    public int purgeExpiredTokens() {
        return blacklistedTokenDao.deleteExpiredBefore(clock.instant());
    }

    private UserDto withRolesAndPermissions(User user) {
        UserDto dto = userMapper.toDto(user);
        dto.setRoles(userDao.findRoleNamesByUserId(user.getId()));
        dto.setPermissions(userDao.findPermissionCodesByUserId(user.getId()));
        return dto;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean exceedsLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    private static void requirePasswordWithinLimit(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessRuleException("Password is required");
        }
        // bcrypt only reads the first 72 bytes: refuse longer input instead of silently truncating it
        if (exceedsLimit(password)) {
            throw new BusinessRuleException("Password must not exceed 72 bytes");
        }
    }
}
