package com.edgareldy.springjdbctutorial.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Checks what WebAppInitializer registers on the ServletContext. The context is never refreshed
 * (the ServletContext is a Mockito mock), so no database is involved.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class WebAppInitializerTest {

    @Test
    void _01_ShouldRegisterDispatcherServletOnRoot_WhenApplicationStarts() {
        ServletContext servletContext = mock(ServletContext.class);
        ServletRegistration.Dynamic servletRegistration = mock(ServletRegistration.Dynamic.class);
        FilterRegistration.Dynamic filterRegistration = mock(FilterRegistration.Dynamic.class);
        when(servletContext.addServlet(eq("dispatcher"), any(Servlet.class))).thenReturn(servletRegistration);
        when(servletContext.addFilter(eq("characterEncodingFilter"), any(Filter.class)))
                .thenReturn(filterRegistration);

        new WebAppInitializer().onStartup(servletContext);

        ArgumentCaptor<Servlet> servlet = ArgumentCaptor.forClass(Servlet.class);
        verify(servletContext).addServlet(eq("dispatcher"), servlet.capture());
        assertThat(servlet.getValue()).isInstanceOf(DispatcherServlet.class);
        verify(servletRegistration).addMapping("/");
        verify(servletRegistration).setLoadOnStartup(1);
    }

    @Test
    void _02_ShouldRegisterContextLoaderListener_WhenApplicationStarts() {
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.addServlet(eq("dispatcher"), any(Servlet.class)))
                .thenReturn(mock(ServletRegistration.Dynamic.class));
        when(servletContext.addFilter(eq("characterEncodingFilter"), any(Filter.class)))
                .thenReturn(mock(FilterRegistration.Dynamic.class));

        new WebAppInitializer().onStartup(servletContext);

        ArgumentCaptor<ContextLoaderListener> listener = ArgumentCaptor.forClass(ContextLoaderListener.class);
        verify(servletContext).addListener(listener.capture());
        assertThat(listener.getValue()).isNotNull();
    }

    @Test
    void _03_ShouldRegisterUtf8EncodingFilterOnEveryUrl_WhenApplicationStarts() {
        ServletContext servletContext = mock(ServletContext.class);
        FilterRegistration.Dynamic filterRegistration = mock(FilterRegistration.Dynamic.class);
        when(servletContext.addServlet(eq("dispatcher"), any(Servlet.class)))
                .thenReturn(mock(ServletRegistration.Dynamic.class));
        when(servletContext.addFilter(eq("characterEncodingFilter"), any(Filter.class)))
                .thenReturn(filterRegistration);

        new WebAppInitializer().onStartup(servletContext);

        ArgumentCaptor<Filter> filter = ArgumentCaptor.forClass(Filter.class);
        verify(servletContext).addFilter(eq("characterEncodingFilter"), filter.capture());
        assertThat(filter.getValue()).isInstanceOf(CharacterEncodingFilter.class);
        verify(filterRegistration).addMappingForUrlPatterns(any(), eq(false), eq("/*"));
    }
}
