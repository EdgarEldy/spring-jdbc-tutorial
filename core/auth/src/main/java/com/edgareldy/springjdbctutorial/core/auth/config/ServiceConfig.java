package com.edgareldy.springjdbctutorial.core.auth.config;

import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.service.ActorProvider;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.AdminBootstrap;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.AuditLoggerImpl;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.AuthServiceImpl;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.ExpiredTokenCleanupJob;
import com.edgareldy.springjdbctutorial.core.auth.service.impl.RbacServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Declares the service beans of the auth module. Imports its DaoConfig so importing this class alone
 * is enough. The Clock bean comes from CommonConfig, the JdbcTemplate from DataSourceConfig. Any context
 * importing this class must also provide an ActorProvider bean (declared by the web layer).
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

    // AuditLogger and RbacService are returned as their interfaces so the @Transactional advice (including
    // the REQUIRES_NEW of logRejected) is applied by the proxy. RbacServiceImpl receives the AuditLogger
    // PROXY here, which is why its refusal rows really commit in an independent transaction.
    // The ActorProvider parameter is a bean the web layer declares: core only knows the interface.
    @Bean
    public AuditLogger auditLogger(AuditLogDao auditLogDao, ActorProvider actorProvider, Clock clock) {
        return new AuditLoggerImpl(auditLogDao, actorProvider, clock);
    }

    @Bean
    public RbacService rbacService(UserDao userDao, RoleDao roleDao, PermissionDao permissionDao,
                                   AuditLogger auditLogger) {
        return new RbacServiceImpl(userDao, roleDao, permissionDao, auditLogger);
    }

    // initMethod "run" makes Spring call AdminBootstrap.run() once the bean is wired, after Flyway (the DAOs
    // depend on it). The account is created only when BOTH app.bootstrap.* values are configured (development
    // only: never set them in test or production); absent values make the bean do nothing. The Environment
    // is read leniently (getProperty, no default), unlike the required datasource settings.
    @Bean(initMethod = "run")
    public AdminBootstrap adminBootstrap(UserDao userDao, RoleDao roleDao, AuditLogger auditLogger,
                                         PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager,
                                         Environment environment) {
        return new AdminBootstrap(userDao, roleDao, auditLogger, passwordEncoder,
                new TransactionTemplate(transactionManager),
                environment.getProperty("app.bootstrap.admin-email"),
                environment.getProperty("app.bootstrap.admin-password"));
    }
}
