package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.mockito.Mockito.mock;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.catalog.service.CategoryService;
import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.ws.config.SecurityConfig;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.controller.CategoryController;
import com.edgareldy.springjdbctutorial.ws.controller.ProductController;
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
 * Shared test context of the catalog controller tests: MVC, the REAL SecurityConfig (JWT filter, method security
 * with CustomPermissionEvaluator, 401/403 handlers), the real controllers and advice, and Mockito mocks in
 * place of CategoryService and ProductService (no database).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as RbacMvcTestConfig, kept in ws.mvc and never in ws.controller so WebMvcConfig's component scan
// cannot pick it up. The real controllers are declared as beans wired to the mocked services.
@Configuration
@EnableWebMvc
@Import(SecurityConfig.class)
public class CatalogMvcTestConfig implements WebMvcConfigurer {

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
    CategoryService categoryService() {
        return mock(CategoryService.class);
    }

    @Bean
    ProductService productService() {
        return mock(ProductService.class);
    }

    @Bean
    CategoryController categoryController(CategoryService categoryService) {
        return new CategoryController(categoryService);
    }

    @Bean
    ProductController productController(ProductService productService) {
        return new ProductController(productService);
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
