package com.edgareldy.springjdbctutorial.ws.integration;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static com.edgareldy.springjdbctutorial.ws.support.AuthTestSupport.PASSWORD;
import static com.edgareldy.springjdbctutorial.ws.support.AuthTestSupport.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import com.edgareldy.springjdbctutorial.ws.support.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Full-stack flows of feature/auth: the real WebMvcConfig (real services, DAOs, Flyway, security chain)
 * against PostgreSQL in Testcontainers, driven through MockMvc. Every test uses its own random email, so
 * tests sharing the one database never collide.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitWebConfig(WebMvcConfig.class)
class AuthFlowIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AuthService authService;

    private AuthTestSupport support;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Same wiring as production: springSecurity() puts the springSecurityFilterChain in front of MVC
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        support = new AuthTestSupport(mockMvc);
    }

    @AfterEach
    void tearDown() {
        support.close();
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    // ---------------------------------------------------------------- the README flow

    @Test
    void _01_ShouldCompleteRegisterActivateLoginMeLogout_WhenFollowingTheHappyPath() throws Exception {
        String email = uniqueEmail("flow");

        JsonNode registered = assertSuccess(support.register(email, PASSWORD), 201);
        assertThat(registered.get("data").get("enabled").asBoolean()).isFalse();
        assertThat(registered.get("data").has("password")).isFalse();

        // The raw token exists only in the log: the database holds its SHA-256 digest, never the raw value
        String rawActivation = support.activationToken(email);
        assertThat(count("SELECT COUNT(*) FROM activation_tokens WHERE token = ?", rawActivation)).isZero();
        assertThat(count("SELECT COUNT(*) FROM activation_tokens WHERE token = ?",
                JwtService.sha256Hex(rawActivation))).isOne();
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = ? AND password = ?", email, PASSWORD)).isZero();

        assertSuccess(support.activate(rawActivation), 200);

        String jwt = support.loginToken(email, PASSWORD);
        JsonNode me = assertSuccess(support.getAs(jwt, "/api/v1/auth/me"), 200);
        assertThat(me.get("data").get("email").asText()).isEqualTo(email);
        assertThat(me.get("data").get("enabled").asBoolean()).isTrue();
        assertThat(me.get("data").has("password")).isFalse();

        assertSuccess(support.logout(jwt), 200);
        assertError(support.getAs(jwt, "/api/v1/auth/me"), 401);
    }

    @Test
    void _02_ShouldRejectOnlyTheLoggedOutToken_WhenTheSameUserHoldsTwoTokens() throws Exception {
        String email = uniqueEmail("blacklist");
        String tokenA = support.registerActivateAndLogin(email, PASSWORD);
        String tokenB = support.loginToken(email, PASSWORD);
        assertThat(tokenB).isNotEqualTo(tokenA);

        assertSuccess(support.logout(tokenA), 200);

        assertError(support.getAs(tokenA, "/api/v1/auth/me"), 401);
        assertSuccess(support.getAs(tokenB, "/api/v1/auth/me"), 200);
        assertThat(count("SELECT COUNT(*) FROM blacklisted_tokens WHERE token = ?", JwtService.sha256Hex(tokenA))).isOne();
        assertThat(count("SELECT COUNT(*) FROM blacklisted_tokens WHERE token = ?", tokenA)).isZero();
    }

    @Test
    void _03_ShouldReturn401AndKeepOneBlacklistRow_WhenTheRevokedTokenLogsOutAgain() throws Exception {
        String email = uniqueEmail("logout-twice");
        String token = support.registerActivateAndLogin(email, PASSWORD);

        assertSuccess(support.logout(token), 200);
        // The revoked token is refused by the filter before the controller runs: no second row, no 500
        assertError(support.logout(token), 401);

        assertThat(count("SELECT COUNT(*) FROM blacklisted_tokens WHERE user_id = "
                + "(SELECT id FROM users WHERE email = ?)", email)).isOne();
    }

    @Test
    void _04_ShouldResetPasswordAndRefuseTheOldOne_WhenForgotAndResetAreFollowed() throws Exception {
        String email = uniqueEmail("reset");
        support.registerActivateAndLogin(email, PASSWORD);

        assertSuccess(support.forgotPassword(email), 200);
        String rawReset = support.resetToken(email);
        assertThat(count("SELECT COUNT(*) FROM password_reset_tokens WHERE token = ?", rawReset)).isZero();
        assertThat(count("SELECT COUNT(*) FROM password_reset_tokens WHERE token = ?",
                JwtService.sha256Hex(rawReset))).isOne();

        assertSuccess(support.resetPassword(rawReset, "Brand-new-pass1"), 200);

        assertError(support.login(email, PASSWORD), 401);
        assertSuccess(support.login(email, "Brand-new-pass1"), 200);
        // A reset token is single use
        assertError(support.resetPassword(rawReset, "Another-pass-2"), 422);
    }

    // ---------------------------------------------------------------- rejections

    @Test
    void _05_ShouldReturn401AndNoToken_WhenAccountIsNotActivatedYet() throws Exception {
        String email = uniqueEmail("inactive");
        support.register(email, PASSWORD);

        JsonNode json = assertError(support.login(email, PASSWORD), 401);

        assertThat(json.get("message").asText()).isEqualTo("Account is not activated or is locked");
    }

    @Test
    void _06_ShouldReturn422_WhenEmailIsAlreadyRegisteredWhateverItsCase() throws Exception {
        String email = uniqueEmail("dup");
        assertSuccess(support.register(email, PASSWORD), 201);

        assertError(support.register(email, PASSWORD), 422);
        assertError(support.register(email.toUpperCase(), PASSWORD), 422);
    }

    @Test
    void _07_ShouldReturn400_WhenPasswordHas73AsciiBytes() throws Exception {
        MvcResult result = support.register(uniqueEmail("long"), "a".repeat(73));

        assertError(result, 400);
    }

    @Test
    void _08_ShouldReturn422NotAServerError_WhenPasswordIsUnder72CharactersButOver72Bytes() throws Exception {
        // 40 x "e acute" passes the 72 character validation but is 80 bytes: the service must refuse it cleanly
        MvcResult result = support.register(uniqueEmail("multibyte"), "é".repeat(40));

        JsonNode json = assertError(result, 422);
        assertThat(json.get("message").asText()).contains("72 bytes");
    }

    @Test
    void _09_ShouldGiveTheSameAnswer_WhenEmailIsUnknownOrPasswordIsWrong() throws Exception {
        String email = uniqueEmail("creds");
        support.registerActivateAndLogin(email, PASSWORD);

        JsonNode wrongPassword = assertError(support.login(email, "Wrong-password-1"), 401);
        JsonNode unknownEmail = assertError(support.login(uniqueEmail("ghost"), PASSWORD), 401);
        JsonNode tooLong = assertError(support.login(email, "x".repeat(200)), 401);

        assertThat(wrongPassword.get("message").asText()).isEqualTo(unknownEmail.get("message").asText());
        assertThat(tooLong.get("message").asText()).isEqualTo(unknownEmail.get("message").asText());
    }

    @Test
    void _10_ShouldGiveTheSameAnswer_WhenForgotPasswordIsCalledForKnownAndUnknownEmails() throws Exception {
        String email = uniqueEmail("forgot");
        support.registerActivateAndLogin(email, PASSWORD);

        MvcResult known = support.forgotPassword(email);
        MvcResult unknown = support.forgotPassword(uniqueEmail("nobody"));

        assertThat(known.getResponse().getStatus()).isEqualTo(200).isEqualTo(unknown.getResponse().getStatus());
        assertThat(assertSuccess(known, 200).get("message").asText())
                .isEqualTo(assertSuccess(unknown, 200).get("message").asText());
    }

    @Test
    void _11_ShouldReturn422_WhenActivationTokenIsUnknownReusedOrExpired() throws Exception {
        String email = uniqueEmail("activation");
        support.register(email, PASSWORD);
        String raw = support.activationToken(email);

        assertError(support.activate("unknown-token"), 422);
        assertSuccess(support.activate(raw), 200);
        assertError(support.activate(raw), 422);

        String expiredEmail = uniqueEmail("expired");
        support.register(expiredEmail, PASSWORD);
        String expiredRaw = support.activationToken(expiredEmail);
        jdbcTemplate.update("UPDATE activation_tokens SET expires_at = now() - interval '1 hour' WHERE token = ?",
                JwtService.sha256Hex(expiredRaw));
        assertError(support.activate(expiredRaw), 422);
        assertError(support.login(expiredEmail, PASSWORD), 401);
    }

    @Test
    void _12_ShouldReturn401WhateverThePath_WhenNoTokenIsSent() throws Exception {
        assertError(mockMvc.perform(get("/api/v1/nothing-here")).andReturn(), 401);
        assertError(mockMvc.perform(get("/api/v1/auth/me")).andReturn(), 401);
    }

    @Test
    void _13_ShouldReturn404_WhenUnknownUrlIsCalledWithAValidToken() throws Exception {
        String token = support.registerActivateAndLogin(uniqueEmail("notfound"), PASSWORD);

        assertError(support.getAs(token, "/api/v1/nothing-here"), 404);
    }

    // ---------------------------------------------------------------- scheduled cleanup

    @Test
    void _14_ShouldDeleteOnlyExpiredBlacklistRows_WhenTheCleanupRuns() throws Exception {
        String email = uniqueEmail("purge");
        JsonNode registered = assertSuccess(support.register(email, PASSWORD), 201);
        long userId = registered.get("data").get("id").asLong();
        String expiredHash = JwtService.sha256Hex(UUID.randomUUID().toString());
        String liveHash = JwtService.sha256Hex(UUID.randomUUID().toString());
        String insert = "INSERT INTO blacklisted_tokens (user_id, token, jti, blacklisted_at, created_at, expires_at) "
                + "VALUES (?, ?, ?, now(), now() - interval '2 days', now() + (?::interval))";
        jdbcTemplate.update(insert, userId, expiredHash, UUID.randomUUID().toString(), "-1 day");
        jdbcTemplate.update(insert, userId, liveHash, UUID.randomUUID().toString(), "1 day");

        int removed = authService.purgeExpiredTokens();

        assertThat(removed).isGreaterThanOrEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM blacklisted_tokens WHERE token = ?", expiredHash)).isZero();
        assertThat(count("SELECT COUNT(*) FROM blacklisted_tokens WHERE token = ?", liveHash)).isOne();
    }
}
