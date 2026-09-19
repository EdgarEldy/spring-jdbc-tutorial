package com.edgareldy.springjdbctutorial.core.auth.config;

import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.ActivationTokenDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.BlacklistedTokenDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.AuditLogDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.PasswordResetTokenDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.PermissionDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.RoleDaoImpl;
import com.edgareldy.springjdbctutorial.core.auth.dao.impl.UserDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Declares the DAO beans of the auth module, typed by interface (no stereotype on the implementations).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
public class DaoConfig {

    @Bean
    public UserDao userDao(JdbcTemplate jdbcTemplate) {
        return new UserDaoImpl(jdbcTemplate);
    }

    @Bean
    public ActivationTokenDao activationTokenDao(JdbcTemplate jdbcTemplate) {
        return new ActivationTokenDaoImpl(jdbcTemplate);
    }

    @Bean
    public PasswordResetTokenDao passwordResetTokenDao(JdbcTemplate jdbcTemplate) {
        return new PasswordResetTokenDaoImpl(jdbcTemplate);
    }

    @Bean
    public BlacklistedTokenDao blacklistedTokenDao(JdbcTemplate jdbcTemplate) {
        return new BlacklistedTokenDaoImpl(jdbcTemplate);
    }

    @Bean
    public RoleDao roleDao(JdbcTemplate jdbcTemplate) {
        return new RoleDaoImpl(jdbcTemplate);
    }

    @Bean
    public PermissionDao permissionDao(JdbcTemplate jdbcTemplate) {
        return new PermissionDaoImpl(jdbcTemplate);
    }

    @Bean
    public AuditLogDao auditLogDao(JdbcTemplate jdbcTemplate) {
        return new AuditLogDaoImpl(jdbcTemplate);
    }
}
