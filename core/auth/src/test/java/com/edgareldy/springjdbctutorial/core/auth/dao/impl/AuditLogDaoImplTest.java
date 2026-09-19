package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests AuditLogDaoImpl against the real PostgreSQL, with its own fixture file audit-log-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/audit-log-dao-dataset.sql")
class AuditLogDaoImplTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:15:30Z");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private AuditLogDao auditLogDao;

    @Test
    void _01_ShouldReturnEntriesOrderedById_WhenFindingAll() {
        List<AuditLog> all = auditLogDao.findAll();

        assertThat(all).extracting(AuditLog::getId).containsExactly(1L, 2L);
        assertThat(all.get(0).getAction()).isEqualTo("CREATE_ROLE");
        assertThat(all.get(0).getActorUserId()).isEqualTo(7L);
        assertThat(all.get(0).getEntityId()).isEqualTo(3L);
        assertThat(all.get(0).getCreatedAt()).isEqualTo(Instant.parse("2026-09-19T09:00:00Z"));
    }

    @Test
    void _02_ShouldReadNullActorAndNullEntityId_WhenTheRowHasNeither() {
        AuditLog row = auditLogDao.findAll().get(1);

        assertThat(row.getActorUserId()).isNull();
        assertThat(row.getEntityId()).isNull();
        assertThat(row.getAction()).isEqualTo("BOOTSTRAP_ADMIN");
    }

    @Test
    void _03_ShouldGenerateIdAndRoundTripEveryField_WhenEntryIsInserted() {
        AuditLog created = auditLogDao.insert(
                new AuditLog(null, 5L, "DELETE_ROLE", "ROLE", 9L, "roleName=EDITOR", NOW));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        AuditLog reloaded = auditLogDao.findAll().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getActorUserId()).isEqualTo(5L);
        assertThat(reloaded.getAction()).isEqualTo("DELETE_ROLE");
        assertThat(reloaded.getEntityType()).isEqualTo("ROLE");
        assertThat(reloaded.getEntityId()).isEqualTo(9L);
        assertThat(reloaded.getDetails()).isEqualTo("roleName=EDITOR");
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void _04_ShouldStoreNulls_WhenActorAndEntityIdAreNull() {
        AuditLog created = auditLogDao.insert(
                new AuditLog(null, null, "REJECTED_CREATE_ROLE", "ROLE", null, null, NOW));

        AuditLog reloaded = auditLogDao.findAll().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getActorUserId()).isNull();
        assertThat(reloaded.getEntityId()).isNull();
        assertThat(reloaded.getDetails()).isNull();
    }

    @Test
    void _05_ShouldPlaceNewEntryLast_WhenFindingAllAfterAnInsert() {
        AuditLog created = auditLogDao.insert(new AuditLog(null, 1L, "CREATE_ROLE", "ROLE", 1L, "d", NOW));

        List<AuditLog> all = auditLogDao.findAll();

        assertThat(all).hasSize(3);
        assertThat(all.get(2).getId()).isEqualTo(created.getId());
    }
}
