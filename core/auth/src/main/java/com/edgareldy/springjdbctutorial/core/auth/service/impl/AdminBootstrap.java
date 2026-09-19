package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Optional first administrator, created at startup only when both configuration values are set.
 * The values are for development only and must never be set in test or production. It never makes
 * startup fail: every problem is logged as a warning. The password is never logged.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AdminBootstrap {

    private static final Log LOG = LogFactory.getLog(AdminBootstrap.class);

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final AuditLogger audit;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final String email;
    private final String password;

    public AdminBootstrap(UserDao userDao, RoleDao roleDao, AuditLogger audit, PasswordEncoder passwordEncoder,
                          TransactionTemplate transactionTemplate, String email, String password) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.email = email;
        this.password = password;
    }

    // Registered with @Bean(initMethod = "run"): Spring calls it once, after the bean is built and its
    // dependencies are wired. Nothing is thrown out of it, so a bootstrap problem cannot stop the application.
    public void run() {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return;
        }
        try {
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
                LOG.warn("Admin bootstrap skipped: the configured password exceeds 72 bytes");
                return;
            }
            // The programmatic counterpart of @Transactional: the user, its role and the audit row are one unit.
            Boolean created = transactionTemplate.execute(status -> createAdmin(normalizedEmail));
            if (Boolean.TRUE.equals(created)) {
                LOG.info("Admin bootstrap created the administrator account " + normalizedEmail);
            }
        } catch (Exception e) {
            // Deliberately broad: whatever goes wrong here (missing ADMIN role, database error) must not abort startup
            LOG.warn("Admin bootstrap failed, startup continues: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
    }

    private Boolean createAdmin(String normalizedEmail) {
        if (userDao.existsByEmail(normalizedEmail)) {
            return Boolean.FALSE;
        }
        Role adminRole = roleDao.findByRoleName(ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("Seeded role " + ADMIN_ROLE + " not found"));
        User user = userDao.insert(new User(null, "Admin", "Admin", normalizedEmail,
                passwordEncoder.encode(password), true, false));
        userDao.addRole(user.getId(), adminRole.getId());
        audit.log("BOOTSTRAP_ADMIN", "USER", user.getId(), "email=" + normalizedEmail);
        return Boolean.TRUE;
    }
}
