package com.edgareldy.springjdbctutorial.ws.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springjdbctutorial.core.auth.service.ActorProvider;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.ws.security.ApiAccessDeniedHandler;
import com.edgareldy.springjdbctutorial.ws.security.ApiAuthenticationEntryPoint;
import com.edgareldy.springjdbctutorial.ws.security.JwtAuthFilter;
import com.edgareldy.springjdbctutorial.ws.security.CustomPermissionEvaluator;
import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import com.edgareldy.springjdbctutorial.ws.security.SecurityContextActorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.time.Clock;

/**
 * Spring Security wiring: the JWT service, the JSON 401/403 handlers and the stateless SecurityFilterChain.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @EnableWebSecurity registers the Spring Security infrastructure and the servlet filter named
// "springSecurityFilterChain" (a FilterChainProxy) that the DelegatingFilterProxy of WebAppInitializer
// delegates to. Every SecurityFilterChain bean declared here becomes one ordered list of security filters.
// @EnableMethodSecurity turns on @PreAuthorize/@PostAuthorize: Spring wraps the beans that carry them in
// AOP proxies (CGLIB class proxies here, controllers implement no interface) whose interceptor evaluates
// the expression before the method runs and throws AccessDeniedException when it is false. Controllers
// carry the rule declaratively, never a manual permission check.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
public class SecurityConfig {

    // Read from the Environment: the two key locations have no default (startup fails clearly when
    // missing), expiration and issuer have harmless defaults.
    @Bean
    public JwtService jwtService(Environment environment, ResourceLoader resourceLoader, Clock clock) {
        return new JwtService(resourceLoader,
                "app.jwt.private-key-location", environment.getProperty("app.jwt.private-key-location"),
                "app.jwt.public-key-location", environment.getProperty("app.jwt.public-key-location"),
                environment.getProperty("app.jwt.expiration-seconds", Long.class, 3600L),
                environment.getProperty("app.jwt.issuer", "spring-jdbc-tutorial"),
                clock);
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new ApiAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler(ObjectMapper objectMapper) {
        return new ApiAccessDeniedHandler(objectMapper);
    }

    // The SecurityFilterChain describes how requests are protected. This is a token API: CSRF protection
    // guards browser sessions and cookies, so it is disabled here, and the chain is STATELESS (no
    // HttpSession is created or read, every request must carry its own Bearer JWT). JwtAuthFilter runs
    // before UsernamePasswordAuthenticationFilter so the SecurityContext is filled before authorization.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   AuthService authService,
                                                   AuthenticationEntryPoint apiAuthenticationEntryPoint,
                                                   AccessDeniedHandler apiAccessDeniedHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/activate-account").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler))
                .addFilterBefore(new JwtAuthFilter(jwtService, authService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CustomPermissionEvaluator customPermissionEvaluator() {
        return new CustomPermissionEvaluator();
    }

    // The expression handler is what evaluates hasPermission(...) inside @PreAuthorize: registering the
    // evaluator on it is how hasPermission('ROLE','WRITE') reaches CustomPermissionEvaluator. The bean
    // method is static because the method security infrastructure (a BeanPostProcessor) needs it very
    // early: a static @Bean can be created without instantiating this configuration class first, which
    // avoids the "not eligible for auto-proxying" warning and early-initialisation problems.
    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }

    @Bean
    public ActorProvider actorProvider() {
        return new SecurityContextActorProvider();
    }
}
