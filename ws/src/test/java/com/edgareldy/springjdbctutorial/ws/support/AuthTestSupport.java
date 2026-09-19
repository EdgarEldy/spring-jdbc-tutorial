package com.edgareldy.springjdbctutorial.ws.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared fixtures for full-stack tests: registers, activates and logs in a user through the real HTTP endpoints
 * and returns the JWT. There is no mailer, so the raw activation and reset tokens are read from the INFO log
 * lines of AuthServiceImpl with a logback appender. Reusable by later branches (admin and simple users).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class AuthTestSupport implements AutoCloseable {

    /** A password accepted by every validation rule. */
    public static final String PASSWORD = "Str0ng-Password!";

    private static final String SERVICE_LOGGER = "com.edgareldy.springjdbctutorial.core.auth.service.impl.AuthServiceImpl";
    private static final Pattern ACTIVATION = Pattern.compile("Activation token for (\\S+): (\\S+)");
    private static final Pattern RESET = Pattern.compile("Password reset token for (\\S+): (\\S+)");

    private final MockMvc mockMvc;
    private final Logger logger;
    // logback's ListAppender keeps every logged event in memory, which lets a test read them back
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public AuthTestSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.logger = (Logger) LoggerFactory.getLogger(SERVICE_LOGGER);
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    /** Detaches the appender: call it from an @AfterEach so log capture never leaks between test classes. */
    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }

    /** An email never used before, so tests sharing one database never collide. */
    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String registerBody(String email, String password) {
        return "{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    public MvcResult register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, password))).andReturn();
    }

    public MvcResult activate(String rawToken) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/activate-account").param("token", rawToken)).andReturn();
    }

    public MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    public MvcResult forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}")).andReturn();
    }

    public MvcResult resetPassword(String rawToken, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"" + newPassword + "\"}")).andReturn();
    }

    public MvcResult logout(String jwt) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + jwt)).andReturn();
    }

    /** GET on a route with the Bearer header, for the common "call as this user" case. */
    public MvcResult getAs(String jwt, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + jwt)).andReturn();
    }

    /** The most recent raw activation token logged for the email. */
    public String activationToken(String email) {
        return lastToken(ACTIVATION, email);
    }

    /** The most recent raw password reset token logged for the email. */
    public String resetToken(String email) {
        return lastToken(RESET, email);
    }

    /** Registers, activates and logs in a brand new user, then returns its JWT. */
    public String registerActivateAndLogin(String email, String password) throws Exception {
        register(email, password);
        activate(activationToken(email));
        return loginToken(email, password);
    }

    /** Logs in (expects success) and returns the JWT of the response. */
    public String loginToken(String email, String password) throws Exception {
        MvcResult result = login(email, password);
        JsonNode json = ApiAssertions.assertSuccess(result, 200);
        return json.get("data").get("token").asText();
    }

    private String lastToken(Pattern pattern, String email) {
        List<ILoggingEvent> events = new ArrayList<>(appender.list);
        String found = null;
        for (ILoggingEvent event : events) {
            Matcher matcher = pattern.matcher(event.getFormattedMessage());
            if (matcher.find() && matcher.group(1).equalsIgnoreCase(email)) {
                found = matcher.group(2);
            }
        }
        if (found == null) {
            throw new AssertionError("No token logged for " + email);
        }
        return found;
    }
}
