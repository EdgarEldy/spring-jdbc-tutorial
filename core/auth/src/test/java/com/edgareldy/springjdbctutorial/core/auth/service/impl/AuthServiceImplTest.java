package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests of AuthServiceImpl: every DAO is a Mockito mock, the password encoder is a real
 * low-cost BCryptPasswordEncoder and the clock is fixed, so expiry rules are deterministic.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension creates the @Mock fields before each test and fails a test whose stubbing is never
// used (strict stubs), which keeps the tests free of dead set-up. No Spring context is started.
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private static final String PASSWORD = "Secret123!";
    private static final String HEX_64 = "[0-9a-f]{64}";

    @Mock
    private UserDao userDao;
    @Mock
    private ActivationTokenDao activationTokenDao;
    @Mock
    private PasswordResetTokenDao passwordResetTokenDao;
    @Mock
    private BlacklistedTokenDao blacklistedTokenDao;

    // A spy wraps the real encoder (cost 4 keeps the tests fast) and records the calls made on it
    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));

    // Computed by a separate encoder, so building a User inside a when(...) never calls the spy
    private final String passwordHash = new BCryptPasswordEncoder(4).encode(PASSWORD);

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userDao, activationTokenDao, passwordResetTokenDao, blacklistedTokenDao,
                encoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static UserDto registration(String email, String password) {
        UserDto dto = new UserDto();
        dto.setFirstName("Alice");
        dto.setLastName("Martin");
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private User storedUser(boolean enabled, boolean locked) {
        return new User(5L, "Alice", "Martin", "alice@example.com", passwordHash, enabled, locked);
    }

    private void stubInsertAssigningId() {
        when(userDao.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
    }

    // ---------------------------------------------------------------- register

    @Test
    void _01_ShouldCreateDisabledUserWithHashedPassword_WhenRegistrationIsValid() {
        stubInsertAssigningId();

        UserDto created = service.register(registration("alice@example.com", PASSWORD));

        assertThat(created.getId()).isEqualTo(42L);
        assertThat(created.getPassword()).isNull();
        assertThat(created.isEnabled()).isFalse();
        ArgumentCaptor<User> stored = ArgumentCaptor.forClass(User.class);
        verify(userDao).insert(stored.capture());
        assertThat(stored.getValue().getPassword()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, stored.getValue().getPassword())).isTrue();
        assertThat(stored.getValue().isEnabled()).isFalse();
        assertThat(stored.getValue().isAccountLocked()).isFalse();
    }

    @Test
    void _02_ShouldTrimAndLowerCaseEmail_WhenRegistering() {
        stubInsertAssigningId();

        UserDto created = service.register(registration("  Alice@Example.COM ", PASSWORD));

        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        verify(userDao).existsByEmail("alice@example.com");
    }

    @Test
    void _03_ShouldStoreOnlyTheSha256OfTheActivationToken_WhenRegistering() {
        stubInsertAssigningId();

        service.register(registration("alice@example.com", PASSWORD));

        ArgumentCaptor<ActivationToken> stored = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenDao).insert(stored.capture());
        // A raw token is 43 base64url characters: a 64 lowercase hex value can only be a SHA-256 digest
        assertThat(stored.getValue().getToken()).matches(HEX_64);
        assertThat(stored.getValue().getUserId()).isEqualTo(42L);
        assertThat(stored.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(24 * 3600));
        assertThat(stored.getValue().getValidatedAt()).isNull();
    }

    @Test
    void _04_ShouldThrowBusinessRuleException_WhenEmailIsAlreadyRegistered() {
        when(userDao.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registration("Alice@example.com", PASSWORD)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email is already registered");

        verify(userDao, never()).insert(any());
        verifyNoInteractions(activationTokenDao);
    }

    @Test
    void _05_ShouldThrowBusinessRuleException_WhenInsertLosesARaceOnTheUniqueEmailIndex() {
        when(userDao.insert(any(User.class))).thenThrow(new DuplicateKeyException("uq_users_email_lower"));

        assertThatThrownBy(() -> service.register(registration("alice@example.com", PASSWORD)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email is already registered");

        verifyNoInteractions(activationTokenDao);
    }

    @Test
    void _06_ShouldThrowBusinessRuleException_WhenPasswordExceeds72BytesWithMultibyteCharacters() {
        // 37 x "e acute" is 37 characters but 74 bytes in UTF-8: a character count would let it through
        String tooLong = "é".repeat(37);

        assertThatThrownBy(() -> service.register(registration("alice@example.com", tooLong)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("72 bytes");

        verify(userDao, never()).insert(any());
    }

    @Test
    void _07_ShouldAcceptPassword_WhenItIsExactly72BytesWithMultibyteCharacters() {
        stubInsertAssigningId();
        String atLimit = "é".repeat(36);

        UserDto created = service.register(registration("alice@example.com", atLimit));

        assertThat(created.getId()).isEqualTo(42L);
    }

    @Test
    void _08_ShouldThrowBusinessRuleException_WhenPasswordIsEmpty() {
        assertThatThrownBy(() -> service.register(registration("alice@example.com", "")))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---------------------------------------------------------------- activateAccount

    @Test
    void _09_ShouldEnableUserAndMarkTokenValidated_WhenTokenIsValid() throws Exception {
        ActivationToken token = new ActivationToken(8L, 5L, sha256("raw-activation"), NOW.minusSeconds(60),
                NOW.plusSeconds(3600), null);
        // The stub is keyed on the SHA-256 of the raw value: the service must look up by hash, never by raw
        when(activationTokenDao.findByTokenHash(sha256("raw-activation"))).thenReturn(Optional.of(token));

        service.activateAccount("raw-activation");

        verify(userDao).updateEnabled(5L, true);
        verify(activationTokenDao).markValidated(8L, NOW);
    }

    @Test
    void _10_ShouldThrowBusinessRuleException_WhenActivationTokenIsUnknown() {
        when(activationTokenDao.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAccount("unknown"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userDao, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void _11_ShouldThrowBusinessRuleException_WhenActivationTokenIsExpired() {
        ActivationToken expired = new ActivationToken(8L, 5L, "h", NOW.minusSeconds(7200), NOW, null);
        when(activationTokenDao.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.activateAccount("raw"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userDao, never()).updateEnabled(anyLong(), anyBoolean());
        verify(activationTokenDao, never()).markValidated(anyLong(), any());
    }

    @Test
    void _12_ShouldThrowBusinessRuleException_WhenActivationTokenIsAlreadyValidated() {
        ActivationToken used = new ActivationToken(8L, 5L, "h", NOW.minusSeconds(60), NOW.plusSeconds(3600),
                NOW.minusSeconds(30));
        when(activationTokenDao.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.activateAccount("raw"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userDao, never()).updateEnabled(anyLong(), anyBoolean());
    }

    // ---------------------------------------------------------------- login

    @Test
    void _13_ShouldReturnUserWithRolesAndPermissionsAndNoPassword_WhenCredentialsAreValid() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(true, false)));
        when(userDao.findRoleNamesByUserId(5L)).thenReturn(List.of("ADMIN"));
        when(userDao.findPermissionCodesByUserId(5L)).thenReturn(List.of("USER:READ", "USER:WRITE"));

        UserDto user = service.login(" Alice@Example.com ", PASSWORD);

        assertThat(user.getId()).isEqualTo(5L);
        assertThat(user.getPassword()).isNull();
        assertThat(user.getRoles()).containsExactly("ADMIN");
        assertThat(user.getPermissions()).containsExactly("USER:READ", "USER:WRITE");
    }

    @Test
    void _14_ShouldThrowInvalidCredentials_WhenEmailIsUnknown() {
        when(userDao.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        // The fake verification runs (one bcrypt check) but never against the submitted password
        verify(encoder, times(1)).matches(eq("x"), anyString());
        verify(encoder, never()).matches(eq(PASSWORD), anyString());
    }

    @Test
    void _15_ShouldThrowTheSameException_WhenPasswordIsWrongAsWhenEmailIsUnknown() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(true, false)));
        when(userDao.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException wrongPassword = catchInvalid(() -> service.login("alice@example.com", "Wrong-pass1"));
        InvalidCredentialsException unknownEmail = catchInvalid(() -> service.login("nobody@example.com", PASSWORD));

        assertThat(wrongPassword).isNotNull();
        assertThat(unknownEmail).isNotNull();
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
        assertThat(wrongPassword.getClass()).isEqualTo(unknownEmail.getClass());
    }

    private static InvalidCredentialsException catchInvalid(Runnable call) {
        try {
            call.run();
        } catch (InvalidCredentialsException e) {
            return e;
        }
        return null;
    }

    @Test
    void _16_ShouldThrowInvalidCredentialsAndNeverReachTheHash_WhenPasswordExceeds72Bytes() {
        User user = storedUser(true, false);
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        String tooLong = "é".repeat(37);

        assertThatThrownBy(() -> service.login("alice@example.com", tooLong))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(encoder, never()).matches(eq(tooLong), anyString());
    }

    @Test
    void _17_ShouldThrowAccountNotActive_WhenPasswordIsCorrectButAccountIsDisabled() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(false, false)));

        assertThatThrownBy(() -> service.login("alice@example.com", PASSWORD))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void _18_ShouldThrowAccountNotActive_WhenPasswordIsCorrectButAccountIsLocked() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(true, true)));

        assertThatThrownBy(() -> service.login("alice@example.com", PASSWORD))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void _19_ShouldThrowInvalidCredentialsNotAccountNotActive_WhenPasswordIsWrongOnADisabledAccount() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(false, false)));

        assertThatThrownBy(() -> service.login("alice@example.com", "Wrong-pass1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void _20_ShouldThrowInvalidCredentials_WhenEmailIsNull() {
        assertThatThrownBy(() -> service.login(null, PASSWORD)).isInstanceOf(InvalidCredentialsException.class);
    }

    // ---------------------------------------------------------------- logout / blacklist

    @Test
    void _21_ShouldStoreTokenHashAndJti_WhenLoggingOut() {
        when(blacklistedTokenDao.existsByTokenHash("hash")).thenReturn(false);
        Instant issued = NOW.minusSeconds(600);
        Instant expires = NOW.plusSeconds(3000);

        service.logout(new BlacklistedTokenDto(5L, "hash", "jti-1", issued, expires));

        ArgumentCaptor<BlacklistedToken> stored = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenDao).insert(stored.capture());
        assertThat(stored.getValue().getToken()).isEqualTo("hash");
        assertThat(stored.getValue().getJti()).isEqualTo("jti-1");
        assertThat(stored.getValue().getUserId()).isEqualTo(5L);
        assertThat(stored.getValue().getBlacklistedAt()).isEqualTo(NOW);
        assertThat(stored.getValue().getCreatedAt()).isEqualTo(issued);
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(expires);
    }

    @Test
    void _22_ShouldInsertNothing_WhenTokenIsAlreadyBlacklisted() {
        when(blacklistedTokenDao.existsByTokenHash("hash")).thenReturn(true);

        service.logout(new BlacklistedTokenDto(5L, "hash", "jti-1", NOW, NOW.plusSeconds(60)));

        verify(blacklistedTokenDao, never()).insert(any());
    }

    @Test
    void _23_ShouldNotThrow_WhenConcurrentLogoutHitsTheUniqueIndex() {
        when(blacklistedTokenDao.existsByTokenHash("hash")).thenReturn(false);
        when(blacklistedTokenDao.insert(any())).thenThrow(new DuplicateKeyException("uq_blacklisted_tokens_jti"));

        assertThatCode(() -> service.logout(new BlacklistedTokenDto(5L, "hash", "jti-1", NOW, NOW.plusSeconds(60))))
                .doesNotThrowAnyException();
    }

    @Test
    void _24_ShouldReturnDaoAnswer_WhenAskingIfTokenIsBlacklisted() {
        when(blacklistedTokenDao.existsByTokenHash("revoked")).thenReturn(true);
        when(blacklistedTokenDao.existsByTokenHash("fresh")).thenReturn(false);

        assertThat(service.isTokenBlacklisted("revoked")).isTrue();
        assertThat(service.isTokenBlacklisted("fresh")).isFalse();
    }

    // ---------------------------------------------------------------- getProfile

    @Test
    void _25_ShouldReturnProfileWithRolesAndPermissions_WhenUserExists() {
        when(userDao.findById(5L)).thenReturn(Optional.of(storedUser(true, false)));
        when(userDao.findRoleNamesByUserId(5L)).thenReturn(List.of("ADMIN"));
        when(userDao.findPermissionCodesByUserId(5L)).thenReturn(List.of("USER:READ"));

        UserDto profile = service.getProfile(5L);

        assertThat(profile.getEmail()).isEqualTo("alice@example.com");
        assertThat(profile.getPassword()).isNull();
        assertThat(profile.getRoles()).containsExactly("ADMIN");
        assertThat(profile.getPermissions()).containsExactly("USER:READ");
    }

    @Test
    void _26_ShouldThrowResourceNotFound_WhenProfileUserDoesNotExist() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- forgotPassword

    @Test
    void _27_ShouldReplaceTokensAndStoreOnlyTheHash_WhenEmailIsKnown() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.of(storedUser(true, false)));

        service.forgotPassword("Alice@Example.com");

        verify(passwordResetTokenDao).deleteByUserId(5L);
        ArgumentCaptor<PasswordResetToken> stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenDao).insert(stored.capture());
        assertThat(stored.getValue().getToken()).matches(HEX_64);
        assertThat(stored.getValue().getUserId()).isEqualTo(5L);
        assertThat(stored.getValue().getType()).isEqualTo("PASSWORD_RESET");
        assertThat(stored.getValue().getExpiryDate()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void _28_ShouldNotThrowAndStillRunTheDelete_WhenEmailIsUnknown() {
        when(userDao.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatCode(() -> service.forgotPassword("nobody@example.com")).doesNotThrowAnyException();

        // Same lookup and same delete as for a known email: only the final insert is skipped
        verify(passwordResetTokenDao).deleteByUserId(anyLong());
        verify(passwordResetTokenDao, never()).insert(any());
    }

    @Test
    void _29_ShouldNotThrow_WhenEmailIsNull() {
        assertThatCode(() -> service.forgotPassword(null)).doesNotThrowAnyException();

        verify(passwordResetTokenDao, never()).insert(any());
    }

    // Non-regression: a blank email used to throw a 422 here, unlike the generic answer for an unknown one
    @Test
    void _30_ShouldNotThrow_WhenEmailIsBlank() {
        assertThatCode(() -> service.forgotPassword("   ")).doesNotThrowAnyException();

        verify(passwordResetTokenDao, never()).insert(any());
    }

    // ---------------------------------------------------------------- resetPassword

    @Test
    void _31_ShouldStoreNewHashAndDeleteResetTokens_WhenTokenIsValid() throws Exception {
        PasswordResetToken token = new PasswordResetToken(3L, 5L, sha256("raw-reset"), "PASSWORD_RESET",
                NOW.plusSeconds(600));
        when(passwordResetTokenDao.findByTokenHash(sha256("raw-reset"))).thenReturn(Optional.of(token));

        service.resetPassword("raw-reset", "Brand-new-pass1");

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userDao).updatePassword(eq(5L), hash.capture());
        assertThat(encoder.matches("Brand-new-pass1", hash.getValue())).isTrue();
        verify(passwordResetTokenDao).deleteByUserId(5L);
    }

    @Test
    void _32_ShouldThrowBusinessRuleException_WhenResetTokenIsExpired() {
        PasswordResetToken expired = new PasswordResetToken(3L, 5L, "h", "PASSWORD_RESET", NOW);
        when(passwordResetTokenDao.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("raw", "Brand-new-pass1"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userDao, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void _33_ShouldThrowBusinessRuleException_WhenResetTokenIsUnknown() {
        when(passwordResetTokenDao.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("raw", "Brand-new-pass1"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userDao, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void _34_ShouldThrowBusinessRuleException_WhenNewPasswordExceeds72Bytes() {
        PasswordResetToken token = new PasswordResetToken(3L, 5L, "h", "PASSWORD_RESET", NOW.plusSeconds(600));
        when(passwordResetTokenDao.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("raw", "é".repeat(37)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("72 bytes");

        verify(userDao, never()).updatePassword(anyLong(), anyString());
        verify(passwordResetTokenDao, never()).deleteByUserId(anyLong());
    }

    // ---------------------------------------------------------------- purgeExpiredTokens

    @Test
    void _35_ShouldDeleteRowsExpiredBeforeNowAndReturnTheCount_WhenPurging() {
        when(blacklistedTokenDao.deleteExpiredBefore(NOW)).thenReturn(3);

        assertThat(service.purgeExpiredTokens()).isEqualTo(3);
    }
}
