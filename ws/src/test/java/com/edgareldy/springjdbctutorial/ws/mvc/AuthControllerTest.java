package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edgareldy.springjdbctutorial.core.auth.dto.BlacklistedTokenDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.exception.AccountNotActiveException;
import com.edgareldy.springjdbctutorial.core.auth.exception.InvalidCredentialsException;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.ws.config.SecurityConfig;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.controller.AuthController;
import com.edgareldy.springjdbctutorial.ws.exception.GlobalExceptionHandler;
import com.edgareldy.springjdbctutorial.ws.security.IssuedToken;
import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import com.edgareldy.springjdbctutorial.ws.support.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MockMvc tests of AuthController with AuthService mocked (no database) but the REAL SecurityConfig: JWT
 * filter, entry point, advice and validation all run, so status codes and the ApiResponse shape are the
 * production ones.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Kept in ws.mvc, never in ws.controller: WebMvcConfig component-scans ws.controller and ws.exception and
// would pick up this test configuration. The JWT key locations come from the system properties surefire
// sets (see ws/pom.xml), read by SecurityConfig through the Spring Environment.
@SpringJUnitWebConfig(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    /**
     * Test context: MVC, the real SecurityConfig (JwtService, filter chain, 401/403 handlers), the real
     * controller and advice, and a Mockito mock in place of the AuthService.
     */
    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig implements WebMvcConfigurer {

        private final WebMvcConfig real = new WebMvcConfig();

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        ObjectMapper objectMapper() {
            return real.objectMapper();
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        AuthController authController(AuthService authService, JwtService jwtService) {
            return new AuthController(authService, jwtService);
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Override
        public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
            real.extendMessageConverters(converters);
        }

        @Override
        public Validator getValidator() {
            return real.getValidator();
        }
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(authService);
        // springSecurity() adds the springSecurityFilterChain bean of the context in front of the
        // DispatcherServlet, exactly like the DelegatingFilterProxy does in Tomcat. Without it MockMvc
        // would bypass the JWT filter and every request would look unauthenticated-but-allowed.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UserDto profile() {
        return new UserDto(5L, "Alice", "Martin", "alice@example.com", null, true, false,
                List.of("ADMIN"), List.of("USER:READ"));
    }

    private IssuedToken issueToken() {
        return jwtService.issue(profile());
    }

    private MvcResult call(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn();
    }

    private MockHttpServletRequestBuilder me(String jwt) {
        return get("/api/v1/auth/me").header("Authorization", "Bearer " + jwt);
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ---------------------------------------------------------------- register

    @Test
    void _01_ShouldReturn201WithProfileAndNoPassword_WhenRegistrationIsValid() throws Exception {
        when(authService.register(any(UserDto.class)))
                .thenReturn(new UserDto(42L, "Alice", "Martin", "alice@example.com", null, false, false,
                        List.of(), List.of()));

        MvcResult result = call(postJson("/api/v1/auth/register",
                "{\"firstName\":\"Alice\",\"lastName\":\"Martin\",\"email\":\"alice@example.com\",\"password\":\"Str0ng-Password!\"}"));

        JsonNode json = assertSuccess(result, 201);
        assertThat(json.get("data").get("id").asLong()).isEqualTo(42L);
        assertThat(json.get("data").get("email").asText()).isEqualTo("alice@example.com");
        assertThat(json.get("data").get("enabled").asBoolean()).isFalse();
        assertThat(json.get("data").has("password")).isFalse();
        ArgumentCaptor<UserDto> sent = ArgumentCaptor.forClass(UserDto.class);
        verify(authService).register(sent.capture());
        assertThat(sent.getValue().getFirstName()).isEqualTo("Alice");
        assertThat(sent.getValue().getPassword()).isEqualTo("Str0ng-Password!");
    }

    @Test
    void _02_ShouldReturn400WithPerFieldMessages_WhenRegistrationBodyIsInvalid() throws Exception {
        MvcResult result = call(postJson("/api/v1/auth/register",
                "{\"firstName\":\"\",\"lastName\":\"Martin\",\"email\":\"not-an-email\",\"password\":\"short\"}"));

        JsonNode json = assertError(result, 400);
        String message = json.get("message").asText();
        assertThat(message).contains("firstName: ").contains("email: ").contains("password: ");
        assertThat(message).doesNotContain("lastName");
        verifyNoInteractions(authService);
    }

    @Test
    void _03_ShouldReturn422_WhenServiceRefusesADuplicateEmail() throws Exception {
        when(authService.register(any(UserDto.class))).thenThrow(new BusinessRuleException("Email is already registered"));

        MvcResult result = call(postJson("/api/v1/auth/register",
                "{\"firstName\":\"Alice\",\"lastName\":\"Martin\",\"email\":\"alice@example.com\",\"password\":\"Str0ng-Password!\"}"));

        JsonNode json = assertError(result, 422);
        assertThat(json.get("message").asText()).isEqualTo("Email is already registered");
    }

    @Test
    void _04_ShouldReturn400_WhenRegistrationJsonIsMalformed() throws Exception {
        MvcResult result = call(postJson("/api/v1/auth/register", "{\"firstName\":"));

        assertError(result, 400);
    }

    @Test
    void _05_ShouldReturn415_WhenRegistrationContentTypeIsNotJson() throws Exception {
        MvcResult result = call(post("/api/v1/auth/register").contentType(MediaType.TEXT_PLAIN).content("hello"));

        assertError(result, 415);
    }

    @Test
    void _06_ShouldReturn405_WhenAuthenticatedUserCallsRegisterWithGet() throws Exception {
        MvcResult result = call(get("/api/v1/auth/register")
                .header("Authorization", "Bearer " + issueToken().getToken()));

        assertError(result, 405);
    }

    // ---------------------------------------------------------------- activate-account

    @Test
    void _07_ShouldReturn200_WhenActivationTokenIsAccepted() throws Exception {
        MvcResult result = call(get("/api/v1/auth/activate-account").param("token", "raw-token"));

        assertSuccess(result, 200);
        verify(authService).activateAccount("raw-token");
    }

    @Test
    void _08_ShouldReturn400_WhenActivationTokenParameterIsMissing() throws Exception {
        MvcResult result = call(get("/api/v1/auth/activate-account"));

        JsonNode json = assertError(result, 400);
        assertThat(json.get("message").asText()).isEqualTo("Missing parameter: token");
    }

    @Test
    void _09_ShouldReturn422_WhenActivationTokenIsRefusedByTheService() throws Exception {
        doThrow(new BusinessRuleException("Invalid or expired activation token"))
                .when(authService).activateAccount("bad");

        assertError(call(get("/api/v1/auth/activate-account").param("token", "bad")), 422);
    }

    // ---------------------------------------------------------------- login

    @Test
    void _10_ShouldReturn200WithAValidJwt_WhenCredentialsAreCorrect() throws Exception {
        when(authService.login("alice@example.com", "Str0ng-Password!")).thenReturn(profile());

        MvcResult result = call(postJson("/api/v1/auth/login",
                "{\"email\":\"alice@example.com\",\"password\":\"Str0ng-Password!\"}"));

        JsonNode json = assertSuccess(result, 200);
        assertThat(json.get("data").get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(Instant.parse(json.get("data").get("expiresAt").asText())).isAfter(Instant.now());
        String token = json.get("data").get("token").asText();
        assertThat(jwtService.parse(token).orElseThrow().getUserId()).isEqualTo(5L);
        assertThat(jwtService.parse(token).orElseThrow().getPermissions()).containsExactly("USER:READ");
    }

    @Test
    void _11_ShouldReturn401WithErrorShape_WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new InvalidCredentialsException());

        JsonNode json = assertError(call(postJson("/api/v1/auth/login",
                "{\"email\":\"alice@example.com\",\"password\":\"wrong\"}")), 401);

        assertThat(json.get("message").asText()).isEqualTo("Invalid email or password");
    }

    @Test
    void _12_ShouldReturn401_WhenAccountIsNotActive() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new AccountNotActiveException());

        JsonNode json = assertError(call(postJson("/api/v1/auth/login",
                "{\"email\":\"alice@example.com\",\"password\":\"Str0ng-Password!\"}")), 401);

        assertThat(json.get("message").asText()).isEqualTo("Account is not activated or is locked");
    }

    @Test
    void _13_ShouldReturn400WithPerFieldMessages_WhenLoginBodyIsBlank() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/auth/login", "{\"email\":\"\",\"password\":\"\"}")), 400);

        assertThat(json.get("message").asText()).contains("email: ").contains("password: ");
        verifyNoInteractions(authService);
    }

    // ---------------------------------------------------------------- /me and the JWT filter

    @Test
    void _14_ShouldReturn200WithProfile_WhenBearerTokenIsValid() throws Exception {
        when(authService.getProfile(5L)).thenReturn(profile());

        JsonNode json = assertSuccess(call(me(issueToken().getToken())), 200);

        assertThat(json.get("data").get("email").asText()).isEqualTo("alice@example.com");
        assertThat(json.get("data").get("roles").get(0).asText()).isEqualTo("ADMIN");
        assertThat(json.get("data").get("permissions").get(0).asText()).isEqualTo("USER:READ");
        assertThat(json.get("data").has("password")).isFalse();
    }

    @Test
    void _15_ShouldReturn401ErrorShape_WhenNoTokenIsSent() throws Exception {
        assertError(call(get("/api/v1/auth/me")), 401);

        verifyNoInteractions(authService);
    }

    @Test
    void _16_ShouldReturn401_WhenTokenIsGarbage() throws Exception {
        assertError(call(me("this-is-not-a-jwt")), 401);
    }

    @Test
    void _17_ShouldReturn401_WhenTokenIsExpired() throws Exception {
        JwtService twoHoursAgo = JwtTestSupport.devJwtService(
                Clock.fixed(Instant.now().minusSeconds(7200), ZoneOffset.UTC));

        assertError(call(me(twoHoursAgo.issue(profile()).getToken())), 401);
    }

    @Test
    void _18_ShouldReturn401_WhenTokenIsSignedWithAnotherRsaKey() throws Exception {
        KeyPair foreign = JwtTestSupport.generateKeyPair();
        String forged = JwtTestSupport.rs256Token(foreign.getPrivate(), 5L, Instant.now(),
                Instant.now().plusSeconds(3600));

        assertError(call(me(forged)), 401);
    }

    @Test
    void _19_ShouldReturn401_WhenTokenIsHs256SignedWithThePublicKey() throws Exception {
        KeyPair pair = JwtTestSupport.generateKeyPair();
        String forged = JwtTestSupport.hs256TokenSignedWithPublicKey(pair.getPublic(), 5L, Instant.now(),
                Instant.now().plusSeconds(3600));

        assertError(call(me(forged)), 401);
    }

    @Test
    void _20_ShouldReturn401_WhenTokenIsUnsignedWithAlgNone() throws Exception {
        String unsigned = JwtTestSupport.unsignedToken(5L, Instant.now(), Instant.now().plusSeconds(3600));

        assertError(call(me(unsigned)), 401);
    }

    @Test
    void _21_ShouldReturn401AndNeverLoadTheProfile_WhenTokenIsBlacklisted() throws Exception {
        String token = issueToken().getToken();
        when(authService.isTokenBlacklisted(JwtService.sha256Hex(token))).thenReturn(true);

        assertError(call(me(token)), 401);

        verify(authService, never()).getProfile(anyLong());
    }

    // ---------------------------------------------------------------- logout

    @Test
    void _22_ShouldBlacklistTheTokenBuiltFromThePrincipal_WhenLoggingOut() throws Exception {
        IssuedToken issued = issueToken();

        assertSuccess(call(post("/api/v1/auth/logout").header("Authorization", "Bearer " + issued.getToken())), 200);

        ArgumentCaptor<BlacklistedTokenDto> sent = ArgumentCaptor.forClass(BlacklistedTokenDto.class);
        verify(authService).logout(sent.capture());
        BlacklistedTokenDto dto = sent.getValue();
        assertThat(dto.getUserId()).isEqualTo(5L);
        assertThat(dto.getTokenHash()).isEqualTo(JwtService.sha256Hex(issued.getToken()));
        assertThat(dto.getTokenHash()).isNotEqualTo(issued.getToken());
        assertThat(dto.getJti()).isEqualTo(jwtService.parse(issued.getToken()).orElseThrow().getJti());
        assertThat(dto.getIssuedAt()).isEqualTo(issued.getIssuedAt());
        assertThat(dto.getExpiresAt()).isEqualTo(issued.getExpiresAt());
    }

    @Test
    void _23_ShouldReturn401AndNotTouchTheService_WhenLoggingOutWithoutToken() throws Exception {
        assertError(call(post("/api/v1/auth/logout")), 401);

        verify(authService, never()).logout(any());
    }

    // ---------------------------------------------------------------- forgot / reset password

    @Test
    void _24_ShouldReturnTheSameStatusAndMessage_WhenEmailIsKnownOrUnknown() throws Exception {
        JsonNode known = assertSuccess(call(postJson("/api/v1/auth/forgot-password",
                "{\"email\":\"alice@example.com\"}")), 200);
        JsonNode unknown = assertSuccess(call(postJson("/api/v1/auth/forgot-password",
                "{\"email\":\"nobody@example.com\"}")), 200);

        assertThat(known.get("message").asText()).isEqualTo(unknown.get("message").asText());
        assertThat(known.has("data") && !known.get("data").isNull()).isFalse();
        assertThat(unknown.has("data") && !unknown.get("data").isNull()).isFalse();
        verify(authService).forgotPassword("alice@example.com");
        verify(authService).forgotPassword("nobody@example.com");
    }

    @Test
    void _25_ShouldReturn400_WhenForgotPasswordEmailIsBlank() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/auth/forgot-password", "{\"email\":\"\"}")), 400);

        assertThat(json.get("message").asText()).contains("email: ");
    }

    @Test
    void _26_ShouldReturn200_WhenResetPasswordIsAccepted() throws Exception {
        assertSuccess(call(postJson("/api/v1/auth/reset-password",
                "{\"token\":\"raw-reset\",\"newPassword\":\"Brand-new-pass1\"}")), 200);

        verify(authService).resetPassword("raw-reset", "Brand-new-pass1");
    }

    @Test
    void _27_ShouldReturn422_WhenResetTokenIsRefusedByTheService() throws Exception {
        doThrow(new BusinessRuleException("Invalid or expired reset token"))
                .when(authService).resetPassword(anyString(), anyString());

        assertError(call(postJson("/api/v1/auth/reset-password",
                "{\"token\":\"bad\",\"newPassword\":\"Brand-new-pass1\"}")), 422);
    }

    @Test
    void _28_ShouldReturn400WithPerFieldMessages_WhenNewPasswordIsTooShort() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/auth/reset-password",
                "{\"token\":\"raw\",\"newPassword\":\"short\"}")), 400);

        assertThat(json.get("message").asText()).contains("newPassword: ");
        verifyNoInteractions(authService);
    }

    // ---------------------------------------------------------------- unknown URL

    @Test
    void _29_ShouldReturn401NotSpecific404_WhenUnknownUrlIsCalledWithoutToken() throws Exception {
        assertError(call(get("/api/v1/does-not-exist")), 401);
    }

    @Test
    void _30_ShouldReturn404ErrorShape_WhenUnknownUrlIsCalledWithAValidToken() throws Exception {
        assertError(call(get("/api/v1/does-not-exist")
                .header("Authorization", "Bearer " + issueToken().getToken())), 404);
    }
}
