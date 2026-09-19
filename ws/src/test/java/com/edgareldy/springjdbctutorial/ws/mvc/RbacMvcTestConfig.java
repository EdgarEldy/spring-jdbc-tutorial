package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.mockito.Mockito.mock;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.ws.config.SecurityConfig;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.controller.PermissionController;
import com.edgareldy.springjdbctutorial.ws.controller.RoleController;
import com.edgareldy.springjdbctutorial.ws.controller.UserController;
import com.edgareldy.springjdbctutorial.ws.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Shared test context of the RBAC controller tests: MVC, the REAL SecurityConfig (JWT filter, method security
 * with CustomPermissionEvaluator, 401/403 handlers), the real controllers and advice, and Mockito mocks in
 * place of RbacService and AuthService (no database).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Kept in ws.mvc, never in ws.controller: WebMvcConfig component-scans ws.controller and ws.exception and
// would pick up this test configuration. Every controller test class points @SpringJUnitWebConfig at this
// class, so Spring's test context cache builds the context once and shares it; each test resets the mocks.
@Configuration
@EnableWebMvc
@Import(SecurityConfig.class)
public class RbacMvcTestConfig implements WebMvcConfigurer {

    private final WebMvcConfig real = new WebMvcConfig();

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ObjectMapper objectMapper() {
        return real.objectMapper();
    }

    // The security filter chain needs an AuthService to ask "is this token blacklisted?"
    @Bean
    AuthService authService() {
        return mock(AuthService.class);
    }

    @Bean
    RbacService rbacService() {
        return mock(RbacService.class);
    }

    @Bean
    UserController userController(RbacService rbacService) {
        return new UserController(rbacService);
    }

    @Bean
    RoleController roleController(RbacService rbacService) {
        return new RoleController(rbacService);
    }

    @Bean
    PermissionController permissionController(RbacService rbacService) {
        return new PermissionController(rbacService);
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
