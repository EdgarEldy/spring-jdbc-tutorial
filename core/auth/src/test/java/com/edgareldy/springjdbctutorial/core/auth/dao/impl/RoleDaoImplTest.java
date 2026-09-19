package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests RoleDaoImpl against the real PostgreSQL, with its own fixture file role-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as the other DAO tests: Testcontainers PostgreSQL migrated by Flyway, a Spring context built
// from the two real configuration classes, the container url injected by @DynamicPropertySource, and this
// class's own fixture file executed by @Sql before EACH test so every test starts from the same rows.
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/role-dao-dataset.sql")
class RoleDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private RoleDao roleDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void _01_ShouldReturnRolesOrderedByName_WhenFindingAll() {
        assertThat(roleDao.findAll()).extracting(Role::getRoleName)
                .containsExactly("ADMIN", "EDITOR", "GUEST", "VIEWER");
    }

    @Test
    void _02_ShouldReturnRole_WhenIdExists() {
        assertThat(roleDao.findById(2L)).get().extracting(Role::getRoleName).isEqualTo("EDITOR");
    }

    @Test
    void _03_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(roleDao.findById(999L)).isEmpty();
    }

    @Test
    void _04_ShouldReturnRole_WhenNameExists() {
        assertThat(roleDao.findByRoleName("VIEWER")).get().extracting(Role::getId).isEqualTo(3L);
    }

    @Test
    void _05_ShouldReturnEmpty_WhenNameDoesNotExist() {
        assertThat(roleDao.findByRoleName("NOBODY")).isEmpty();
    }

    @Test
    void _06_ShouldReportExistence_WhenCheckingByName() {
        assertThat(roleDao.existsByRoleName("ADMIN")).isTrue();
        assertThat(roleDao.existsByRoleName("NOBODY")).isFalse();
    }

    @Test
    void _07_ShouldGenerateIdAndPersistRow_WhenRoleIsInserted() {
        Role created = roleDao.insert(new Role(null, "AUDITOR"));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        assertThat(roleDao.findById(created.getId())).get().extracting(Role::getRoleName).isEqualTo("AUDITOR");
    }

    @Test
    void _08_ShouldThrowDuplicateKeyException_WhenRoleNameAlreadyExists() {
        assertThatThrownBy(() -> roleDao.insert(new Role(null, "ADMIN"))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _09_ShouldRenameRole_WhenNameIsUpdated() {
        roleDao.updateName(4L, "VISITOR");

        assertThat(roleDao.findById(4L)).get().extracting(Role::getRoleName).isEqualTo("VISITOR");
    }

    @Test
    void _10_ShouldThrowDuplicateKeyException_WhenRenamingToAnExistingName() {
        assertThatThrownBy(() -> roleDao.updateName(4L, "ADMIN")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _11_ShouldDeleteRole_WhenNoUserHoldsIt() {
        roleDao.delete(4L);

        assertThat(roleDao.findById(4L)).isEmpty();
    }

    @Test
    void _12_ShouldDeleteItsPermissionAssignmentsToo_WhenRoleIsDeleted() {
        roleDao.delete(3L);

        Long left = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role_permission WHERE role_id = 3", Long.class);
        assertThat(left).isZero();
    }

    @Test
    void _13_ShouldThrowDataIntegrityViolationException_WhenRoleIsStillAssignedToAUser() {
        assertThatThrownBy(() -> roleDao.delete(2L)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(roleDao.findById(2L)).isPresent();
    }

    @Test
    void _14_ShouldCountAssignedUsers_WhenCountingUsersWithRole() {
        assertThat(roleDao.countUsersWithRole(1L)).isEqualTo(1L);
        assertThat(roleDao.countUsersWithRole(2L)).isEqualTo(2L);
        assertThat(roleDao.countUsersWithRole(3L)).isZero();
    }

    @Test
    void _15_ShouldReturnPermissionsOrderedByResourceThenAction_WhenReadingThePermissionsOfARole() {
        assertThat(roleDao.findPermissionsByRoleId(1L)).extracting(p -> p.getResource() + ":" + p.getAction())
                .containsExactly("ROLE:READ", "ROLE:WRITE", "USER:READ", "USER:WRITE");
    }

    @Test
    void _16_ShouldReturnEmptyList_WhenRoleHasNoPermission() {
        assertThat(roleDao.findPermissionsByRoleId(4L)).isEmpty();
    }

    @Test
    void _17_ShouldAssignPermission_WhenAddingIt() {
        roleDao.addPermission(4L, 2L);

        assertThat(roleDao.hasPermission(4L, 2L)).isTrue();
        assertThat(roleDao.findPermissionsByRoleId(4L)).extracting(Permission::getId).containsExactly(2L);
    }

    @Test
    void _18_ShouldThrowDuplicateKeyException_WhenPermissionIsAlreadyAssigned() {
        assertThatThrownBy(() -> roleDao.addPermission(1L, 1L)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _19_ShouldThrowDataIntegrityViolationException_WhenPermissionDoesNotExist() {
        assertThatThrownBy(() -> roleDao.addPermission(4L, 999L)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _20_ShouldReturnOneThenZero_WhenRemovingTheSamePermissionTwice() {
        assertThat(roleDao.removePermission(1L, 4L)).isEqualTo(1);
        assertThat(roleDao.removePermission(1L, 4L)).isZero();
        assertThat(roleDao.hasPermission(1L, 4L)).isFalse();
    }

    @Test
    void _21_ShouldReportAssignment_WhenCheckingHasPermission() {
        assertThat(roleDao.hasPermission(2L, 1L)).isTrue();
        assertThat(roleDao.hasPermission(2L, 2L)).isFalse();
    }
}
