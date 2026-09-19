package com.edgareldy.springjdbctutorial.ws.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.edgareldy.springjdbctutorial.core.common.config.CommonConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC configuration and single entry point of the wiring (imports the core module configs).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @EnableWebMvc activates Spring MVC (handler mappings, argument resolvers, message converters) in
// plain Spring, without Boot. The WebMvcConfigurer callbacks below customise it.
//
// Core module configs are imported here in the SAME context (one shared context). CommonConfig comes
// first; each later branch adds its module's ServiceConfig (which imports its own DaoConfig) to the @Import
// list, fully qualified because every module uses the same simple names. SecurityConfig is imported too.
//
// The component scan is limited to the web layer (controllers and the advice): those are the only
// stereotypes of the project. DAO and service implementations are never scanned, they are explicit
// @Bean methods, so nothing gets wired by accident.
@Configuration
@EnableWebMvc
@Import({CommonConfig.class, com.edgareldy.springjdbctutorial.core.auth.config.ServiceConfig.class, SecurityConfig.class})
@ComponentScan({"com.edgareldy.springjdbctutorial.ws.controller", "com.edgareldy.springjdbctutorial.ws.exception"})
public class WebMvcConfig implements WebMvcConfigurer {

    // ONE shared ObjectMapper bean: the MVC converter below and the security handlers (401/403 bodies,
    // written outside MVC) must serialise ApiResponse identically. The builder registers the JSR-310
    // module found on the classpath; disabling WRITE_DATES_AS_TIMESTAMPS makes Instant serialise as
    // ISO-8601 text instead of a number. Null fields stay in the output.
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    // Swaps the default Jackson converter for one built on the shared ObjectMapper and keeps every
    // other default converter (String, byte[], Resource...).
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper()));
    }

    // LocalValidatorFactoryBean bootstraps Bean Validation (Hibernate Validator on the classpath).
    // Declared as a bean so Spring manages its lifecycle and injects the context; getValidator()
    // returns that same instance so @Valid on request bodies uses it.
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Override
    public Validator getValidator() {
        return validator();
    }
}
