package com.edgareldy.springjdbctutorial.ws.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.edgareldy.springjdbctutorial.core.common.config.CommonConfig;
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
// first; each later branch adds its module's DaoConfig and ServiceConfig to the @Import list.
//
// The component scan is limited to the web layer (controllers and the advice): those are the only
// stereotypes of the project. DAO and service implementations are never scanned, they are explicit
// @Bean methods, so nothing gets wired by accident.
@Configuration
@EnableWebMvc
@Import(CommonConfig.class)
@ComponentScan({"com.edgareldy.springjdbctutorial.ws.controller", "com.edgareldy.springjdbctutorial.ws.exception"})
public class WebMvcConfig implements WebMvcConfigurer {

    // Replaces the default converters with a single Jackson one. The builder registers the JSR-310
    // module (jackson-datatype-jsr310) found on the classpath; disabling WRITE_DATES_AS_TIMESTAMPS
    // makes Instant serialise as ISO-8601 text instead of a number. Null fields stay in the output.
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }

    // LocalValidatorFactoryBean bootstraps Bean Validation (Hibernate Validator on the classpath).
    // Returning it from getValidator() makes @Valid on request bodies use it.
    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
