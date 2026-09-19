package com.edgareldy.springjdbctutorial.ws.config;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.EnumSet;

/**
 * Programmatic replacement of web.xml: builds the Spring context and registers servlet and filters.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Tomcat finds every WebApplicationInitializer through Spring's SpringServletContainerInitializer
// (Servlet 3+ mechanism) and calls onStartup, so no web.xml is needed.
//
// ONE AnnotationConfigWebApplicationContext is shared as the root context (ContextLoaderListener) AND
// as the DispatcherServlet's context. Everything (DAOs, services, controllers, security) then lives in
// one place, so a bean is defined once and the security filter, which is created outside the
// DispatcherServlet, can find it through the root context.
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(WebMvcConfig.class);

        servletContext.addListener(new ContextLoaderListener(context));

        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(context));
        dispatcher.setLoadOnStartup(1);
        dispatcher.setAsyncSupported(true);
        dispatcher.addMapping("/");

        FilterRegistration.Dynamic encoding =
                servletContext.addFilter("characterEncodingFilter", new CharacterEncodingFilter("UTF-8", true));
        encoding.setAsyncSupported(true);
        encoding.addMappingForUrlPatterns(EnumSet.of(jakarta.servlet.DispatcherType.REQUEST), false, "/*");
    }
}
