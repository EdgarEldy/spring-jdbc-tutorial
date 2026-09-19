package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Unit tests of RbacServiceImpl: every DAO and the AuditLogger are Mockito mocks, assertions are made on the dto
 * objects returned, on the audit calls (success row, REJECTED_ row, none for a 404) and on the order of the
 * last-admin protocol (advisory lock first, count before and after).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension builds the @Mock fields before each test and fails a test whose stubbing is never used
// (strict stubs). The service is a plain object here: no Spring context, no transaction, so the
// @Transactional advice is proved by the integration tests instead.
@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    private static final String LAST_ADMIN = "Cannot remove the last account holding ROLE:WRITE";

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private PermissionDao permissionDao;
    @Mock
    private AuditLogger audit;

    private RbacServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RbacServiceImpl(userDao, roleDao, permissionDao, audit);
    }

    private static User alice() {
        return new User(5L, "Alice", "Martin", "alice@example.com", "secret-hash", true, false);
    }

    private static Role editor() {
        return new Role(3L, "EDITOR");
    }

    private static Permission roleWrite() {
        return new Permission(7L, "ROLE", "WRITE");
    }

    private void stubRoleInsert(long id) {
        when(roleDao.insert(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(id);
            return role;
        });
    }

    private void stubPermissionInsert(long id) {
        when(permissionDao.insert(any(Permission.class))).thenAnswer(invocation -> {
            Permission permission = invocation.getArgument(0);
            permission.setId(id);
            return permission;
        });
    }

    private static RoleDto roleInput(String name) {
        return new RoleDto(null, name, null);
    }

    private static PermissionDto permissionInput(String resource, String action) {
        return new PermissionDto(null, resource, action);
    }

    // ------------------------------------------------------------------ listUsers

    @Test
    void _01_ShouldReturnPageWithRolesAndPermissions_WhenPageAndSizeAreValid() {
        when(userDao.findPage(1, 2)).thenReturn(List.of(alice()));
        when(userDao.countAll()).thenReturn(3L);
        when(userDao.findRoleNamesByUserId(5L)).thenReturn(List.of("ADMIN"));
        when(userDao.findPermissionCodesByUserId(5L)).thenReturn(List.of("ROLE:WRITE"));

        PageDto<UserDto> page = service.listUsers(1, 2);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getContent()).hasSize(1);
        UserDto dto = page.getContent().get(0);
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getPassword()).isNull();
        assertThat(dto.getRoles()).containsExactly("ADMIN");
        assertThat(dto.getPermissions()).containsExactly("ROLE:WRITE");
    }

    @Test
    void _02_ShouldReturnEmptyContentButRealTotal_WhenPageIsBeyondTheLastOne() {
        when(userDao.findPage(50, 10)).thenReturn(List.of());
        when(userDao.countAll()).thenReturn(3L);

        PageDto<UserDto> page = service.listUsers(50, 10);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _03_ShouldRefuseAndNotQuery_WhenPageIsNegative() {
        assertThatThrownBy(() -> service.listUsers(-1, 10)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(userDao, audit);
    }

    @Test
    void _04_ShouldRefuseAndNotQuery_WhenSizeIsZero() {
        assertThatThrownBy(() -> service.listUsers(0, 0)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(userDao, audit);
    }

    @Test
    void _05_ShouldRefuseAndNotQuery_WhenSizeExceedsOneHundred() {
        assertThatThrownBy(() -> service.listUsers(0, 101)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(userDao, audit);
    }

    @Test
    void _06_ShouldAcceptBothBounds_WhenSizeIsOneAndOneHundred() {
        when(userDao.findPage(0, 1)).thenReturn(List.of());
        when(userDao.findPage(0, 100)).thenReturn(List.of());

        assertThat(service.listUsers(0, 1).getSize()).isEqualTo(1);
        assertThat(service.listUsers(0, 100).getSize()).isEqualTo(100);
    }

    // ------------------------------------------------------------------ getUser

    @Test
    void _07_ShouldReturnUserWithRoles_WhenUserExists() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(userDao.findRoleNamesByUserId(5L)).thenReturn(List.of("EDITOR"));

        UserDto dto = service.getUser(5L);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getRoles()).containsExactly("EDITOR");
        assertThat(dto.getPassword()).isNull();
    }

    @Test
    void _08_ShouldThrowNotFoundWithoutAuditing_WhenUserDoesNotExist() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
    }

    // ------------------------------------------------------------------ assignRoleToUser

    @Test
    void _09_ShouldAssignAndAuditSuccess_WhenRoleIsNotYetAssigned() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.hasRole(5L, 3L)).thenReturn(false);
        when(userDao.findRoleNamesByUserId(5L)).thenReturn(List.of("EDITOR"));

        UserDto dto = service.assignRoleToUser(5L, 3L);

        verify(userDao).addRole(5L, 3L);
        verify(audit).log("ASSIGN_ROLE_TO_USER", "USER", 5L, "role=EDITOR, userId=5");
        verify(audit, never()).logRejected(anyString(), anyString(), any(), anyString());
        assertThat(dto.getRoles()).containsExactly("EDITOR");
    }

    @Test
    void _10_ShouldAuditRefusalAndNotAssign_WhenRoleIsAlreadyAssigned() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.hasRole(5L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.assignRoleToUser(5L, 3L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Role is already assigned to this user");

        verify(audit).logRejected("ASSIGN_ROLE_TO_USER", "USER", 5L,
                "role=EDITOR, userId=5, reason=Role is already assigned to this user");
        verify(userDao, never()).addRole(anyLong(), anyLong());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _11_ShouldTurnRaceIntoBusinessRuleException_WhenAddRoleThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("pk");
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.hasRole(5L, 3L)).thenReturn(false);
        doThrow(race).when(userDao).addRole(5L, 3L);

        assertThatThrownBy(() -> service.assignRoleToUser(5L, 3L))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _12_ShouldThrowNotFoundWithoutAuditing_WhenUserToAssignToDoesNotExist() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRoleToUser(99L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void _13_ShouldThrowNotFoundWithoutAuditing_WhenRoleToAssignDoesNotExist() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRoleToUser(5L, 99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(userDao, never()).addRole(anyLong(), anyLong());
    }

    // ------------------------------------------------------------------ removeRoleFromUser

    @Test
    void _14_ShouldTakeTheLockFirstThenCountBeforeRemovalAndAfter_WhenRemovingARole() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(2L, 2L);
        when(userDao.removeRole(5L, 3L)).thenReturn(1);

        service.removeRoleFromUser(5L, 3L);

        // The advisory lock must precede every read, otherwise two concurrent removals could both see "2 admins"
        InOrder order = inOrder(userDao, roleDao);
        order.verify(userDao).acquireLastAdminLock();
        order.verify(userDao).findById(5L);
        order.verify(roleDao).findById(3L);
        order.verify(userDao).countLastAdminCandidates();
        order.verify(userDao).removeRole(5L, 3L);
        order.verify(userDao).countLastAdminCandidates();
    }

    @Test
    void _15_ShouldAuditSuccess_WhenAnotherHolderRemains() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(2L, 1L);
        when(userDao.removeRole(5L, 3L)).thenReturn(1);

        UserDto dto = service.removeRoleFromUser(5L, 3L);

        verify(audit).log("REMOVE_ROLE_FROM_USER", "USER", 5L, "role=EDITOR, userId=5");
        verify(audit, never()).logRejected(anyString(), anyString(), any(), anyString());
        assertThat(dto.getId()).isEqualTo(5L);
    }

    @Test
    void _16_ShouldAuditRefusalAndThrow_WhenRemovalLeavesNoRoleWriteHolder() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L, 0L);
        when(userDao.removeRole(5L, 3L)).thenReturn(1);

        assertThatThrownBy(() -> service.removeRoleFromUser(5L, 3L))
                .isInstanceOf(BusinessRuleException.class).hasMessage(LAST_ADMIN);

        verify(audit).logRejected("REMOVE_ROLE_FROM_USER", "USER", 5L,
                "role=EDITOR, userId=5, reason=" + LAST_ADMIN);
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _17_ShouldNotRefuse_WhenThereWasNoRoleWriteHolderBeforeTheRemoval() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(0L, 0L);
        when(userDao.removeRole(5L, 3L)).thenReturn(1);

        service.removeRoleFromUser(5L, 3L);

        verify(audit).log("REMOVE_ROLE_FROM_USER", "USER", 5L, "role=EDITOR, userId=5");
    }

    @Test
    void _18_ShouldNotRefuse_WhenTheOnlyHolderIsUnaffectedByTheRemoval() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L, 1L);
        when(userDao.removeRole(5L, 3L)).thenReturn(1);

        service.removeRoleFromUser(5L, 3L);

        verify(audit).log("REMOVE_ROLE_FROM_USER", "USER", 5L, "role=EDITOR, userId=5");
    }

    @Test
    void _19_ShouldThrowNotFoundWithoutAuditing_WhenRoleIsNotAssignedToTheUser() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L);
        when(userDao.removeRole(5L, 3L)).thenReturn(0);

        assertThatThrownBy(() -> service.removeRoleFromUser(5L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void _20_ShouldTakeTheLockThenThrowNotFoundWithoutAuditing_WhenUserDoesNotExist() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeRoleFromUser(99L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        InOrder order = inOrder(userDao);
        order.verify(userDao).acquireLastAdminLock();
        order.verify(userDao).findById(99L);
        verifyNoInteractions(audit);
    }

    @Test
    void _21_ShouldThrowNotFoundWithoutAuditing_WhenRoleToRemoveDoesNotExist() {
        when(userDao.findById(5L)).thenReturn(Optional.of(alice()));
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeRoleFromUser(5L, 99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(userDao, never()).removeRole(anyLong(), anyLong());
    }

    // ------------------------------------------------------------------ listRoles

    @Test
    void _22_ShouldReturnEveryRoleWithItsPermissions_WhenListingRoles() {
        when(roleDao.findAll()).thenReturn(List.of(editor(), new Role(4L, "GUEST")));
        when(roleDao.findPermissionsByRoleId(3L)).thenReturn(List.of(roleWrite()));
        when(roleDao.findPermissionsByRoleId(4L)).thenReturn(List.of());

        List<RoleDto> roles = service.listRoles();

        assertThat(roles).extracting(RoleDto::getRoleName).containsExactly("EDITOR", "GUEST");
        assertThat(roles.get(0).getPermissions()).extracting(PermissionDto::getResource).containsExactly("ROLE");
        assertThat(roles.get(1).getPermissions()).isEmpty();
    }

    // ------------------------------------------------------------------ createRole

    @Test
    void _23_ShouldTrimInsertAndAuditSuccess_WhenRoleNameIsNew() {
        when(roleDao.existsByRoleName("Reviewer")).thenReturn(false);
        stubRoleInsert(9L);

        RoleDto dto = service.createRole(roleInput("  Reviewer  "));

        ArgumentCaptor<Role> inserted = ArgumentCaptor.forClass(Role.class);
        verify(roleDao).insert(inserted.capture());
        assertThat(inserted.getValue().getRoleName()).isEqualTo("Reviewer");
        verify(audit).log("CREATE_ROLE", "ROLE", 9L, "roleName=Reviewer");
        assertThat(dto.getId()).isEqualTo(9L);
        assertThat(dto.getRoleName()).isEqualTo("Reviewer");
    }

    @Test
    void _24_ShouldAuditRefusalAndNotInsert_WhenRoleNameAlreadyExists() {
        when(roleDao.existsByRoleName("EDITOR")).thenReturn(true);

        assertThatThrownBy(() -> service.createRole(roleInput("EDITOR")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Role name already exists");

        verify(audit).logRejected("CREATE_ROLE", "ROLE", null, "roleName=EDITOR, reason=Role name already exists");
        verify(roleDao, never()).insert(any());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _25_ShouldTurnRaceIntoBusinessRuleException_WhenInsertThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("uq_roles_role_name");
        when(roleDao.existsByRoleName("EDITOR")).thenReturn(false);
        when(roleDao.insert(any(Role.class))).thenThrow(race);

        assertThatThrownBy(() -> service.createRole(roleInput("EDITOR")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _26_ShouldRefuseWithoutAuditOrDaoCall_WhenRoleNameIsNullBlankOrInputIsNull() {
        assertThatThrownBy(() -> service.createRole(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createRole(roleInput(null))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createRole(roleInput("   "))).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(roleDao, audit);
    }

    @Test
    void _27_ShouldRefuseAtLengthOneHundredAndOne_WhenRoleNameIsTooLong() {
        assertThatThrownBy(() -> service.createRole(roleInput("x".repeat(101))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("100");

        verifyNoInteractions(roleDao, audit);
    }

    @Test
    void _28_ShouldAcceptLengthOneHundred_WhenRoleNameIsAtTheLimit() {
        String name = "x".repeat(100);
        when(roleDao.existsByRoleName(name)).thenReturn(false);
        stubRoleInsert(10L);

        assertThat(service.createRole(roleInput(name)).getRoleName()).isEqualTo(name);
    }

    // ------------------------------------------------------------------ updateRole

    @Test
    void _29_ShouldRenameAndAuditSuccess_WhenNewNameIsFree() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.existsByRoleName("Editors")).thenReturn(false);

        RoleDto dto = service.updateRole(3L, roleInput(" Editors "));

        verify(roleDao).updateName(3L, "Editors");
        verify(audit).log("UPDATE_ROLE", "ROLE", 3L, "roleName=EDITOR -> Editors");
        assertThat(dto.getRoleName()).isEqualTo("Editors");
    }

    @Test
    void _30_ShouldNotCheckUniquenessAgainstItself_WhenNameIsUnchanged() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));

        service.updateRole(3L, roleInput("EDITOR"));

        verify(roleDao, never()).existsByRoleName(anyString());
        verify(roleDao).updateName(3L, "EDITOR");
        verify(audit).log("UPDATE_ROLE", "ROLE", 3L, "roleName=EDITOR -> EDITOR");
    }

    @Test
    void _31_ShouldAuditRefusalAndNotRename_WhenNewNameBelongsToAnotherRole() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.existsByRoleName("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.updateRole(3L, roleInput("ADMIN")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Role name already exists");

        verify(audit).logRejected("UPDATE_ROLE", "ROLE", 3L,
                "roleName=EDITOR -> ADMIN, reason=Role name already exists");
        verify(roleDao, never()).updateName(anyLong(), anyString());
    }

    @Test
    void _32_ShouldTurnRaceIntoBusinessRuleException_WhenUpdateThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("uq_roles_role_name");
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.existsByRoleName("ADMIN")).thenReturn(false);
        doThrow(race).when(roleDao).updateName(3L, "ADMIN");

        assertThatThrownBy(() -> service.updateRole(3L, roleInput("ADMIN")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _33_ShouldThrowNotFoundWithoutAuditing_WhenRoleToUpdateDoesNotExist() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole(99L, roleInput("X"))).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(roleDao, never()).updateName(anyLong(), anyString());
    }

    @Test
    void _34_ShouldRefuseWithoutRenaming_WhenUpdatedNameIsBlank() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));

        assertThatThrownBy(() -> service.updateRole(3L, roleInput(" "))).isInstanceOf(BusinessRuleException.class);

        verify(roleDao, never()).updateName(anyLong(), anyString());
        verifyNoInteractions(audit);
    }

    // ------------------------------------------------------------------ deleteRole

    @Test
    void _35_ShouldDeleteAndAuditSuccess_WhenNoUserHoldsTheRole() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.countUsersWithRole(3L)).thenReturn(0L);

        service.deleteRole(3L);

        verify(roleDao).delete(3L);
        verify(audit).log("DELETE_ROLE", "ROLE", 3L, "roleName=EDITOR");
    }

    @Test
    void _36_ShouldAuditRefusalAndNotDelete_WhenUsersStillHoldTheRole() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.countUsersWithRole(3L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteRole(3L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Role is still assigned to users");

        verify(audit).logRejected("DELETE_ROLE", "ROLE", 3L,
                "roleName=EDITOR, reason=Role is still assigned to users");
        verify(roleDao, never()).delete(anyLong());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _37_ShouldTurnRaceIntoBusinessRuleException_WhenTheForeignKeyRefusesTheDelete() {
        DataIntegrityViolationException race = new DataIntegrityViolationException("fk_role_user_role");
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(roleDao.countUsersWithRole(3L)).thenReturn(0L);
        doThrow(race).when(roleDao).delete(3L);

        assertThatThrownBy(() -> service.deleteRole(3L)).isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected("DELETE_ROLE", "ROLE", 3L, "roleName=EDITOR, reason=Role is still assigned to users");
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _38_ShouldThrowNotFoundWithoutAuditing_WhenRoleToDeleteDoesNotExist() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRole(99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(roleDao, never()).delete(anyLong());
    }

    // ------------------------------------------------------------------ assignPermissionToRole

    @Test
    void _39_ShouldAssignAndAuditSuccess_WhenPermissionIsNotYetAssigned() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(roleDao.hasPermission(3L, 7L)).thenReturn(false);
        when(roleDao.findPermissionsByRoleId(3L)).thenReturn(List.of(roleWrite()));

        RoleDto dto = service.assignPermissionToRole(3L, 7L);

        verify(roleDao).addPermission(3L, 7L);
        verify(audit).log("ASSIGN_PERMISSION_TO_ROLE", "ROLE", 3L, "role=EDITOR, permission=ROLE:WRITE");
        assertThat(dto.getPermissions()).extracting(PermissionDto::getAction).containsExactly("WRITE");
    }

    @Test
    void _40_ShouldAuditRefusalAndNotAssign_WhenPermissionIsAlreadyAssigned() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(roleDao.hasPermission(3L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.assignPermissionToRole(3L, 7L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Permission is already assigned to this role");

        verify(audit).logRejected("ASSIGN_PERMISSION_TO_ROLE", "ROLE", 3L,
                "role=EDITOR, permission=ROLE:WRITE, reason=Permission is already assigned to this role");
        verify(roleDao, never()).addPermission(anyLong(), anyLong());
    }

    @Test
    void _41_ShouldTurnRaceIntoBusinessRuleException_WhenAddPermissionThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("pk");
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(roleDao.hasPermission(3L, 7L)).thenReturn(false);
        doThrow(race).when(roleDao).addPermission(3L, 7L);

        assertThatThrownBy(() -> service.assignPermissionToRole(3L, 7L))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _42_ShouldThrowNotFoundWithoutAuditing_WhenRoleOrPermissionToAssignDoesNotExist() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignPermissionToRole(99L, 7L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.assignPermissionToRole(3L, 99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(roleDao, never()).addPermission(anyLong(), anyLong());
    }

    // ------------------------------------------------------------------ removePermissionFromRole

    @Test
    void _43_ShouldTakeTheLockFirstThenCountBeforeRemovalAndAfter_WhenRemovingAPermission() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(userDao.countLastAdminCandidates()).thenReturn(2L, 1L);
        when(roleDao.removePermission(3L, 7L)).thenReturn(1);

        service.removePermissionFromRole(3L, 7L);

        InOrder order = inOrder(userDao, roleDao, permissionDao);
        order.verify(userDao).acquireLastAdminLock();
        order.verify(roleDao).findById(3L);
        order.verify(permissionDao).findById(7L);
        order.verify(userDao).countLastAdminCandidates();
        order.verify(roleDao).removePermission(3L, 7L);
        order.verify(userDao).countLastAdminCandidates();
        verify(audit).log("REMOVE_PERMISSION_FROM_ROLE", "ROLE", 3L, "role=EDITOR, permission=ROLE:WRITE");
    }

    @Test
    void _44_ShouldAuditRefusalAndThrow_WhenRemovingRoleWriteFromTheLastAdminRole() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L, 0L);
        when(roleDao.removePermission(3L, 7L)).thenReturn(1);

        assertThatThrownBy(() -> service.removePermissionFromRole(3L, 7L))
                .isInstanceOf(BusinessRuleException.class).hasMessage(LAST_ADMIN);

        verify(audit).logRejected("REMOVE_PERMISSION_FROM_ROLE", "ROLE", 3L,
                "role=EDITOR, permission=ROLE:WRITE, reason=" + LAST_ADMIN);
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _45_ShouldNotRefuse_WhenThereWasNoRoleWriteHolderBeforeTheRemoval() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(userDao.countLastAdminCandidates()).thenReturn(0L, 0L);
        when(roleDao.removePermission(3L, 7L)).thenReturn(1);

        service.removePermissionFromRole(3L, 7L);

        verify(audit).log("REMOVE_PERMISSION_FROM_ROLE", "ROLE", 3L, "role=EDITOR, permission=ROLE:WRITE");
    }

    @Test
    void _46_ShouldThrowNotFoundWithoutAuditing_WhenPermissionIsNotAssignedToTheRole() {
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L);
        when(roleDao.removePermission(3L, 7L)).thenReturn(0);

        assertThatThrownBy(() -> service.removePermissionFromRole(3L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void _47_ShouldTakeTheLockThenThrowNotFoundWithoutAuditing_WhenRoleOrPermissionDoesNotExist() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());
        when(roleDao.findById(3L)).thenReturn(Optional.of(editor()));
        when(permissionDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removePermissionFromRole(99L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.removePermissionFromRole(3L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userDao, times(2)).acquireLastAdminLock();
        verifyNoInteractions(audit);
        verify(roleDao, never()).removePermission(anyLong(), anyLong());
    }

    // ------------------------------------------------------------------ permissions

    @Test
    void _48_ShouldReturnEveryPermission_WhenListingPermissions() {
        when(permissionDao.findAll()).thenReturn(List.of(roleWrite(), new Permission(8L, "USER", "READ")));

        List<PermissionDto> permissions = service.listPermissions();

        assertThat(permissions).extracting(p -> p.getResource() + ":" + p.getAction())
                .containsExactly("ROLE:WRITE", "USER:READ");
    }

    @Test
    void _49_ShouldNormalizeToUpperCaseInsertAndAudit_WhenPermissionIsNew() {
        when(permissionDao.existsByResourceAndAction("ROLE", "WRITE")).thenReturn(false);
        stubPermissionInsert(12L);

        PermissionDto dto = service.createPermission(permissionInput(" role ", "write"));

        ArgumentCaptor<Permission> inserted = ArgumentCaptor.forClass(Permission.class);
        verify(permissionDao).insert(inserted.capture());
        assertThat(inserted.getValue().getResource()).isEqualTo("ROLE");
        assertThat(inserted.getValue().getAction()).isEqualTo("WRITE");
        verify(audit).log("CREATE_PERMISSION", "PERMISSION", 12L, "permission=ROLE:WRITE");
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getResource()).isEqualTo("ROLE");
    }

    @Test
    void _50_ShouldAuditRefusalAndNotInsert_WhenPermissionAlreadyExists() {
        when(permissionDao.existsByResourceAndAction("ROLE", "WRITE")).thenReturn(true);

        assertThatThrownBy(() -> service.createPermission(permissionInput("role", "write")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Permission already exists");

        verify(audit).logRejected("CREATE_PERMISSION", "PERMISSION", null,
                "permission=ROLE:WRITE, reason=Permission already exists");
        verify(permissionDao, never()).insert(any());
    }

    @Test
    void _51_ShouldTurnRaceIntoBusinessRuleException_WhenPermissionInsertThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("uq_permissions_resource_action");
        when(permissionDao.existsByResourceAndAction("ROLE", "WRITE")).thenReturn(false);
        when(permissionDao.insert(any(Permission.class))).thenThrow(race);

        assertThatThrownBy(() -> service.createPermission(permissionInput("ROLE", "WRITE")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _52_ShouldRefuseWithoutAuditOrDaoCall_WhenPermissionInputIsNullBlankOrHasForbiddenCharacters() {
        assertThatThrownBy(() -> service.createPermission(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createPermission(permissionInput(null, "READ")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Resource");
        assertThatThrownBy(() -> service.createPermission(permissionInput("USER", " ")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Action");
        assertThatThrownBy(() -> service.createPermission(permissionInput("USER1", "READ")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Resource");
        assertThatThrownBy(() -> service.createPermission(permissionInput("USER", "RE-AD")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Action");
        assertThatThrownBy(() -> service.createPermission(permissionInput("A".repeat(101), "READ")))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(permissionDao, audit);
    }

    @Test
    void _53_ShouldAcceptUnderscores_WhenResourceIsMadeOfSeveralWords() {
        when(permissionDao.existsByResourceAndAction("AUDIT_LOG", "READ")).thenReturn(false);
        stubPermissionInsert(13L);

        assertThat(service.createPermission(permissionInput("audit_log", "read")).getResource()).isEqualTo("AUDIT_LOG");
    }

    @Test
    void _54_ShouldTakeTheLockFirstThenCountAroundTheUpdate_WhenUpdatingAPermission() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.existsByResourceAndAction("ROLE", "MANAGE")).thenReturn(false);
        when(userDao.countLastAdminCandidates()).thenReturn(2L, 2L);

        PermissionDto dto = service.updatePermission(7L, permissionInput("role", "manage"));

        InOrder order = inOrder(userDao, permissionDao);
        order.verify(userDao).acquireLastAdminLock();
        order.verify(permissionDao).findById(7L);
        order.verify(permissionDao).existsByResourceAndAction("ROLE", "MANAGE");
        order.verify(userDao).countLastAdminCandidates();
        order.verify(permissionDao).update(any(Permission.class));
        order.verify(userDao).countLastAdminCandidates();
        ArgumentCaptor<Permission> updated = ArgumentCaptor.forClass(Permission.class);
        verify(permissionDao).update(updated.capture());
        assertThat(updated.getValue().getId()).isEqualTo(7L);
        assertThat(updated.getValue().getAction()).isEqualTo("MANAGE");
        verify(audit).log("UPDATE_PERMISSION", "PERMISSION", 7L, "permission=ROLE:WRITE -> ROLE:MANAGE");
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getAction()).isEqualTo("MANAGE");
    }

    @Test
    void _55_ShouldAuditRefusalAndThrow_WhenUpdateStripsRoleWriteFromTheLastAdmin() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.existsByResourceAndAction("ROLE", "MANAGE")).thenReturn(false);
        when(userDao.countLastAdminCandidates()).thenReturn(1L, 0L);

        assertThatThrownBy(() -> service.updatePermission(7L, permissionInput("ROLE", "MANAGE")))
                .isInstanceOf(BusinessRuleException.class).hasMessage(LAST_ADMIN);

        verify(audit).logRejected("UPDATE_PERMISSION", "PERMISSION", 7L,
                "permission=ROLE:WRITE -> ROLE:MANAGE, reason=" + LAST_ADMIN);
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _56_ShouldAuditRefusalAndNotUpdate_WhenNewPairBelongsToAnotherPermission() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.existsByResourceAndAction("USER", "READ")).thenReturn(true);

        assertThatThrownBy(() -> service.updatePermission(7L, permissionInput("USER", "READ")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Permission already exists");

        verify(audit).logRejected("UPDATE_PERMISSION", "PERMISSION", 7L,
                "permission=ROLE:WRITE -> USER:READ, reason=Permission already exists");
        verify(permissionDao, never()).update(any());
    }

    @Test
    void _57_ShouldNotCheckUniquenessAgainstItself_WhenResourceAndActionAreUnchanged() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(userDao.countLastAdminCandidates()).thenReturn(1L, 1L);

        service.updatePermission(7L, permissionInput("role", "write"));

        verify(permissionDao, never()).existsByResourceAndAction(anyString(), anyString());
        verify(permissionDao).update(any(Permission.class));
        verify(audit).log("UPDATE_PERMISSION", "PERMISSION", 7L, "permission=ROLE:WRITE -> ROLE:WRITE");
    }

    @Test
    void _58_ShouldTurnRaceIntoBusinessRuleException_WhenPermissionUpdateThrowsDuplicateKey() {
        DuplicateKeyException race = new DuplicateKeyException("uq_permissions_resource_action");
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.existsByResourceAndAction("ROLE", "MANAGE")).thenReturn(false);
        when(userDao.countLastAdminCandidates()).thenReturn(1L);
        doThrow(race).when(permissionDao).update(any(Permission.class));

        assertThatThrownBy(() -> service.updatePermission(7L, permissionInput("ROLE", "MANAGE")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected(anyString(), anyString(), any(), anyString());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _59_ShouldTakeTheLockThenThrowNotFoundWithoutAuditing_WhenPermissionToUpdateDoesNotExist() {
        when(permissionDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePermission(99L, permissionInput("ROLE", "MANAGE")))
                .isInstanceOf(ResourceNotFoundException.class);

        InOrder order = inOrder(userDao, permissionDao);
        order.verify(userDao).acquireLastAdminLock();
        order.verify(permissionDao).findById(99L);
        verifyNoInteractions(audit);
    }

    @Test
    void _60_ShouldDeleteAndAuditSuccess_WhenNoRoleHoldsThePermission() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.countRolesWithPermission(7L)).thenReturn(0L);

        service.deletePermission(7L);

        verify(permissionDao).delete(7L);
        verify(audit).log("DELETE_PERMISSION", "PERMISSION", 7L, "permission=ROLE:WRITE");
    }

    @Test
    void _61_ShouldAuditRefusalAndNotDelete_WhenRolesStillHoldThePermission() {
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.countRolesWithPermission(7L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deletePermission(7L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Permission is still assigned to roles");

        verify(audit).logRejected("DELETE_PERMISSION", "PERMISSION", 7L,
                "permission=ROLE:WRITE, reason=Permission is still assigned to roles");
        verify(permissionDao, never()).delete(anyLong());
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _62_ShouldTurnRaceIntoBusinessRuleException_WhenTheForeignKeyRefusesThePermissionDelete() {
        DataIntegrityViolationException race = new DataIntegrityViolationException("fk_role_permission_permission");
        when(permissionDao.findById(7L)).thenReturn(Optional.of(roleWrite()));
        when(permissionDao.countRolesWithPermission(7L)).thenReturn(0L);
        doThrow(race).when(permissionDao).delete(7L);

        assertThatThrownBy(() -> service.deletePermission(7L)).isInstanceOf(BusinessRuleException.class).hasCause(race);

        verify(audit).logRejected("DELETE_PERMISSION", "PERMISSION", 7L,
                "permission=ROLE:WRITE, reason=Permission is still assigned to roles");
        verify(audit, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void _63_ShouldThrowNotFoundWithoutAuditing_WhenPermissionToDeleteDoesNotExist() {
        when(permissionDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePermission(99L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(audit);
        verify(permissionDao, never()).delete(anyLong());
    }
}
