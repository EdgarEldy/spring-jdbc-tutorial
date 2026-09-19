package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.mockito.Mockito.mock;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.ws.config.SecurityConfig;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.controller.CustomerController;
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
 * Shared test context of the customer controller tests: MVC, the REAL SecurityConfig (JWT filter, method security
 * with CustomPermissionEvaluator, 401/403 handlers), the real controller and advice, and a Mockito mock in place
 * of CustomerService (no database).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as CatalogMvcTestConfig, kept in ws.mvc and never in ws.controller so WebMvcConfig's component scan
// cannot pick it up.
@Configuration
@EnableWebMvc
@Import(SecurityConfig.class)
public class CustomerMvcTestConfig implements WebMvcConfigurer {

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
    CustomerService customerService() {
        return mock(CustomerService.class);
    }

    @Bean
    CustomerController customerController(CustomerService customerService) {
        return new CustomerController(customerService);
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
