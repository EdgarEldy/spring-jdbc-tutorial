package com.edgareldy.springjdbctutorial.core.auth.config;

import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.AuthServiceImpl;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.ExpiredTokenCleanupJob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/**
 * Declares the service beans of the auth module. Imports its DaoConfig so importing this class alone
 * is enough. The Clock bean comes from CommonConfig, the JdbcTemplate from DataSourceConfig.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
// @EnableScheduling registers the post-processor that finds @Scheduled methods on beans and runs them
@EnableScheduling
@Import(DaoConfig.class)
public class ServiceConfig {

    // PasswordEncoder is Spring Security's abstraction for one-way password hashing. BCrypt is adaptive
    // (tunable cost) and salted per hash, so equal passwords give different hashes and brute force is
    // slow. Only the spring-security-crypto artifact is needed, not the whole security stack.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Returned as the AuthService interface: Spring wraps the bean in a JDK dynamic proxy that applies
    // the @Transactional advice, which only works when callers go through the interface.
    @Bean
    public AuthService authService(UserDao userDao, ActivationTokenDao activationTokenDao,
                                   PasswordResetTokenDao passwordResetTokenDao,
                                   BlacklistedTokenDao blacklistedTokenDao, PasswordEncoder passwordEncoder,
                                   Clock clock) {
        return new AuthServiceImpl(userDao, activationTokenDao, passwordResetTokenDao, blacklistedTokenDao,
                passwordEncoder, clock);
    }

    @Bean
    public ExpiredTokenCleanupJob expiredTokenCleanupJob(AuthService authService) {
        return new ExpiredTokenCleanupJob(authService);
    }
}
