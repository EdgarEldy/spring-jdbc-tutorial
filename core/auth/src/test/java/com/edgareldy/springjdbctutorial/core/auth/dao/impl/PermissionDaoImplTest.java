package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests PermissionDaoImpl against the real PostgreSQL, with its own fixture file permission-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/permission-dao-dataset.sql")
class PermissionDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private PermissionDao permissionDao;

    @Test
    void _01_ShouldReturnPermissionsOrderedByResourceThenAction_WhenFindingAll() {
        assertThat(permissionDao.findAll()).extracting(p -> p.getResource() + ":" + p.getAction())
                .containsExactly("ROLE:READ", "ROLE:WRITE", "USER:READ", "USER:WRITE");
    }

    @Test
    void _02_ShouldReturnPermission_WhenIdExists() {
        Permission permission = permissionDao.findById(4L).orElseThrow();

        assertThat(permission.getResource()).isEqualTo("ROLE");
        assertThat(permission.getAction()).isEqualTo("WRITE");
    }

    @Test
    void _03_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(permissionDao.findById(999L)).isEmpty();
    }

    @Test
    void _04_ShouldReportExistence_WhenCheckingByResourceAndAction() {
        assertThat(permissionDao.existsByResourceAndAction("USER", "READ")).isTrue();
        assertThat(permissionDao.existsByResourceAndAction("USER", "DELETE")).isFalse();
    }

    @Test
    void _05_ShouldGenerateIdAndPersistRow_WhenPermissionIsInserted() {
        Permission created = permissionDao.insert(new Permission(null, "CATEGORY", "READ"));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        Permission reloaded = permissionDao.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getResource()).isEqualTo("CATEGORY");
        assertThat(reloaded.getAction()).isEqualTo("READ");
    }

    @Test
    void _06_ShouldThrowDuplicateKeyException_WhenResourceAndActionAlreadyExist() {
        assertThatThrownBy(() -> permissionDao.insert(new Permission(null, "USER", "READ")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _07_ShouldAllowSameResourceWithAnotherAction_WhenOnlyTheActionDiffers() {
        Permission created = permissionDao.insert(new Permission(null, "USER", "DELETE"));

        assertThat(permissionDao.existsByResourceAndAction("USER", "DELETE")).isTrue();
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void _08_ShouldPersistNewValues_WhenPermissionIsUpdated() {
        permissionDao.update(new Permission(2L, "ACCOUNT", "MANAGE"));

        Permission reloaded = permissionDao.findById(2L).orElseThrow();
        assertThat(reloaded.getResource()).isEqualTo("ACCOUNT");
        assertThat(reloaded.getAction()).isEqualTo("MANAGE");
    }

    @Test
    void _09_ShouldThrowDuplicateKeyException_WhenUpdatingToAnExistingPair() {
        assertThatThrownBy(() -> permissionDao.update(new Permission(2L, "USER", "READ")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _10_ShouldDeletePermission_WhenNoRoleHoldsIt() {
        permissionDao.delete(2L);

        assertThat(permissionDao.findById(2L)).isEmpty();
    }

    @Test
    void _11_ShouldThrowDataIntegrityViolationException_WhenAtLeastOneRoleStillHoldsIt() {
        assertThatThrownBy(() -> permissionDao.delete(1L)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(permissionDao.findById(1L)).isPresent();
    }

    @Test
    void _12_ShouldCountHoldingRoles_WhenCountingRolesWithPermission() {
        assertThat(permissionDao.countRolesWithPermission(1L)).isEqualTo(2L);
        assertThat(permissionDao.countRolesWithPermission(4L)).isEqualTo(1L);
        assertThat(permissionDao.countRolesWithPermission(3L)).isZero();
    }
}
