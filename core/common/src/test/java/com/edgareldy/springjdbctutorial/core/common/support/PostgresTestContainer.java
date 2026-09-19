package com.edgareldy.springjdbctutorial.core.common.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton PostgreSQL 16 Testcontainer shared by every DB test of the JVM (published in the common test-jar).
 * The container is started once, on first use, and killed by Testcontainers' Ryuk when the JVM exits.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Testcontainers starts a throwaway real PostgreSQL in Docker, so DAO tests run real SQL against the
// real Flyway schema instead of an in-memory fake. The container lives in a static holder class
// (initialised lazily, once, by the JVM class loader) so all test classes share one database instead
// of paying the startup cost per class. Tests therefore must isolate the global state they touch.
public final class PostgresTestContainer {

    private PostgresTestContainer() {
    }

    /** Lazy holder: the container is created and started the first time this class is loaded. */
    private static final class Holder {
        static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

        static {
            CONTAINER.start();
        }
    }

    /**
     * Returns the running container, starting it on first call.
     */
    public static PostgreSQLContainer<?> get() {
        return Holder.CONTAINER;
    }

    /**
     * Registers app.datasource.url/username/password. Call it from a {@code @DynamicPropertySource}
     * method: Spring evaluates the suppliers lazily, after the container is up, and adds them to the
     * test context Environment (the container port is random, so it cannot be written statically).
     */
    public static void register(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.url", () -> get().getJdbcUrl());
        registry.add("app.datasource.username", () -> get().getUsername());
        registry.add("app.datasource.password", () -> get().getPassword());
    }
}
