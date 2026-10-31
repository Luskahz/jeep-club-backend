package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.iam.authorization.core.application.service.rolepermission.AdminRolePermissionService;
import com.jeepclub.backend.iam.authorization.core.application.exception.permission.*;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.application.exception.rolepermission.*;
import com.jeepclub.backend.iam.authorization.core.domain.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.domain.enums.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.shared.authorization.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminRolePermissionServiceTest {
    static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    final RoleRepository roles = mock(RoleRepository.class);
    final PermissionRepository permissions = mock(PermissionRepository.class);
    final RolePermissionRepository bindings = mock(RolePermissionRepository.class);
    final AdminRolePermissionService service = new AdminRolePermissionService(roles, permissions, bindings, Clock.fixed(NOW, ZoneOffset.UTC));
    final Permission permission = Permission.reconstitute(5L, PermissionCode.AUTHORIZATION_ROLE_READ, "Read roles", ModuleCode.AUTHORIZATION, NOW, NOW);
    Role role(RoleKind kind, RoleStatus status) {
        return Role.reconstitute(1L, "Role", null, kind, status, NOW, NOW, null, status == RoleStatus.ARCHIVED ? NOW : null);
    }
    void existing(Role role) {
        when(roles.findByIdForUpdate(1L)).thenReturn(Optional.of(role));
        when(permissions.findById(5L)).thenReturn(Optional.of(permission));
    }
    @Test void listRequiresRoleAndPreservesPermissionIdentity() {
        assertThatThrownBy(() -> service.findPermissionsByRoleId(1L)).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(bindings);
        when(roles.findById(1L)).thenReturn(Optional.of(role(RoleKind.CUSTOM, RoleStatus.ACTIVE)));
        when(bindings.findPermissionsByRoleId(1L)).thenReturn(List.of(permission));
        assertThat(service.findPermissionsByRoleId(1L).permissions()).containsExactly(permission);
    }
    @Test void assignmentPersistsTheRequestedPermissionAtTheClockInstant() {
        existing(role(RoleKind.CUSTOM, RoleStatus.ACTIVE));
        service.assignPermissionToRole(1L, 5L);
        verify(bindings).save(argThat(b -> b.getId() == null && b.getRoleId().equals(1L)
                && b.getPermissionId().equals(5L) && b.getCreatedAt().equals(NOW)));
    }
    @Test void missingRolePreventsLookupOrMutationOfPermissionBindings() {
        assertThatThrownBy(() -> service.assignPermissionToRole(1L, 5L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.removePermissionFromRole(1L, 5L)).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(permissions, bindings);
    }
    @Test void missingPermissionPreventsAssignmentAndRemoval() {
        when(roles.findByIdForUpdate(1L)).thenReturn(Optional.of(role(RoleKind.CUSTOM, RoleStatus.ACTIVE)));
        assertThatThrownBy(() -> service.assignPermissionToRole(1L, 5L)).isInstanceOf(PermissionNotFoundException.class);
        assertThatThrownBy(() -> service.removePermissionFromRole(1L, 5L)).isInstanceOf(PermissionNotFoundException.class);
        verifyNoInteractions(bindings);
    }
    @Test void rootPermissionsCannotBeAssignedOrRemovedManually() {
        existing(role(RoleKind.ROOT, RoleStatus.ACTIVE));
        assertThatThrownBy(() -> service.assignPermissionToRole(1L, 5L)).isInstanceOf(RootRoleCannotBeChangedException.class);
        assertThatThrownBy(() -> service.removePermissionFromRole(1L, 5L)).isInstanceOf(RootRoleCannotBeChangedException.class);
        verifyNoInteractions(bindings);
    }
    @ParameterizedTest @EnumSource(value=RoleStatus.class, names={"INACTIVE","ARCHIVED"})
    void inactiveAndArchivedRolesDoNotGainNewPermissions(RoleStatus status) {
        existing(role(RoleKind.CUSTOM, status));
        assertThatThrownBy(() -> service.assignPermissionToRole(1L, 5L)).isInstanceOf(
                status == RoleStatus.ARCHIVED ? RoleLifecycleConflictException.class : InactiveRoleCannotBeUsedException.class);
        verifyNoInteractions(bindings);
    }
    @Test void duplicatePermissionIsConflictWithoutSave() {
        existing(role(RoleKind.CUSTOM, RoleStatus.ACTIVE));
        when(bindings.existsByRoleIdAndPermissionId(1L, 5L)).thenReturn(true);
        assertThatThrownBy(() -> service.assignPermissionToRole(1L, 5L)).isInstanceOf(RolePermissionAlreadyExistsException.class);
        verify(bindings, never()).save(any());
    }
    @Test void removalRequiresBindingAndCanReducePrivilegesOnAnInactiveRole() {
        existing(role(RoleKind.CUSTOM, RoleStatus.INACTIVE));
        assertThatThrownBy(() -> service.removePermissionFromRole(1L, 5L)).isInstanceOf(RolePermissionNotFoundException.class);
        verify(bindings, never()).deleteByRoleIdAndPermissionId(any(), any());
        when(bindings.existsByRoleIdAndPermissionId(1L, 5L)).thenReturn(true);
        service.removePermissionFromRole(1L, 5L);
        verify(bindings).deleteByRoleIdAndPermissionId(1L, 5L);
    }
    @Test void archivedRoleMustBeUnarchivedBeforeChangingPermissionBindings() {
        existing(role(RoleKind.CUSTOM, RoleStatus.ARCHIVED));
        assertThatThrownBy(() -> service.removePermissionFromRole(1L, 5L)).isInstanceOf(RoleLifecycleConflictException.class);
        verifyNoInteractions(bindings);
    }
}
