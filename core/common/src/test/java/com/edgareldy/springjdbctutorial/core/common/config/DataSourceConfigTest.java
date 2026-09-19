package com.edgareldy.springjdbctutorial.core.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * Checks that startup fails with a clear message when a required datasource setting is missing.
 * No database is needed: the failure happens while the DataSource bean is being created.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class DataSourceConfigTest {

    // MockEnvironment (from spring-test) is an Environment holding only the properties we give it,
    // so the test does not depend on the developer's real environment variables.
    private static void startWith(MockEnvironment environment) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(DataSourceConfig.class);
        try {
            context.refresh();
        } finally {
            context.close();
        }
    }

    @Test
    void _01_ShouldFailStartup_WhenDatasourceUrlIsMissing() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> startWith(environment))
                .isInstanceOf(BeanCreationException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.datasource.url");
    }

    @Test
    void _02_ShouldFailStartup_WhenDatasourcePasswordIsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.datasource.url", "jdbc:postgresql://localhost:1/x")
                .withProperty("app.datasource.username", "u");

        assertThatThrownBy(() -> startWith(environment))
                .isInstanceOf(BeanCreationException.class)
                .rootCause()
                .hasMessageContaining("app.datasource.password");
    }

    @Test
    void _03_ShouldNotMentionAnyDefaultPassword_WhenStartupFails() {
        MockEnvironment environment = new MockEnvironment();

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> startWith(environment));

        assertThat(failure).isNotNull();
        assertThat(failure.getMessage()).doesNotContain("password=");
    }
}
