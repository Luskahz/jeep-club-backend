package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.iam.authorization.core.application.service.userrole.AdminUserRoleService;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.*;
import com.jeepclub.backend.iam.authorization.core.domain.exception.role.InactiveRoleCannotBeUsedException;
import com.jeepclub.backend.iam.authorization.core.domain.enums.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.port.UserIdentityPort;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminUserRoleServiceTest {
    static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    final UserRoleRepository bindings = mock(UserRoleRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final UserIdentityPort users = mock(UserIdentityPort.class);
    final AdminUserRoleService service = new AdminUserRoleService(bindings, roles, users, Clock.fixed(NOW, ZoneOffset.UTC));

    Role role(long id, RoleKind kind, RoleStatus status) {
        return Role.reconstitute(id, "Role " + id, null, kind, status, NOW, NOW, null,
                status == RoleStatus.ARCHIVED ? NOW : null);
    }
    void existing(Role role) {
        when(users.existsByIdForUpdate(10L)).thenReturn(true);
        when(roles.findByIdForUpdate(role.getId())).thenReturn(Optional.of(role));
    }

    @Test void readRequiresAnExistingIdentityAndReturnsItsRoles() {
        assertThatThrownBy(() -> service.findRolesByUserId(10L)).isInstanceOf(AuthorizationUserNotFoundException.class);
        verifyNoInteractions(bindings);
        when(users.existsById(10L)).thenReturn(true);
        var role = role(1, RoleKind.CUSTOM, RoleStatus.ACTIVE);
        when(bindings.findRolesByUserId(10L)).thenReturn(List.of(role));
        assertThat(service.findRolesByUserId(10L).roles()).containsExactly(role);
    }
    @Test void assignUsesTheLockedIdentityAndPersistsOnlyTheRequestedBindingAtTheClockInstant() {
        existing(role(1, RoleKind.CUSTOM, RoleStatus.ACTIVE));
        service.assignRoleToUser(10L, 1L);
        verify(bindings).save(argThat(b -> b.getId() == null && b.getUserId().equals(10L)
                && b.getRoleId().equals(1L) && b.getCreatedAt().equals(NOW)));
        var order = inOrder(users, roles, bindings);
        order.verify(users).existsByIdForUpdate(10L);
        order.verify(roles).findByIdForUpdate(1L);
        order.verify(bindings).existsByUserIdAndRoleId(10L, 1L);
    }
    @Test void missingIdentityPreventsEveryMutationBeforeRoleLookup() {
        assertThatThrownBy(() -> service.assignRoleToUser(10L, 1L)).isInstanceOf(AuthorizationUserNotFoundException.class);
        assertThatThrownBy(() -> service.revokeRoleFromUser(10L, 1L)).isInstanceOf(AuthorizationUserNotFoundException.class);
        assertThatThrownBy(() -> service.replaceUserRoles(10L, List.of(1L))).isInstanceOf(AuthorizationUserNotFoundException.class);
        verifyNoInteractions(roles, bindings);
    }
    @Test void unknownRoleDoesNotChangeBindings() {
        when(users.existsByIdForUpdate(10L)).thenReturn(true);
        assertThatThrownBy(() -> service.assignRoleToUser(10L, 999L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.revokeRoleFromUser(10L, 999L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.replaceUserRoles(10L, List.of(999L))).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(bindings);
    }
    @Test void rootCannotBeAssignedRevokedOrRequestedAsReplacement() {
        existing(role(1, RoleKind.ROOT, RoleStatus.ACTIVE));
        assertThatThrownBy(() -> service.assignRoleToUser(10L, 1L)).isInstanceOf(RootRoleCannotBeManagedManuallyException.class);
        assertThatThrownBy(() -> service.revokeRoleFromUser(10L, 1L)).isInstanceOf(RootRoleCannotBeManagedManuallyException.class);
        assertThatThrownBy(() -> service.replaceUserRoles(10L, List.of(1L))).isInstanceOf(RootRoleCannotBeManagedManuallyException.class);
        verifyNoInteractions(bindings);
    }
    @ParameterizedTest @EnumSource(value=RoleStatus.class, names={"INACTIVE","ARCHIVED"})
    void inactiveRolesCannotBeAssignedOrIncludedInReplacement(RoleStatus status) {
        existing(role(1, RoleKind.CUSTOM, status));
        assertThatThrownBy(() -> service.assignRoleToUser(10L, 1L)).isInstanceOf(InactiveRoleCannotBeUsedException.class);
        assertThatThrownBy(() -> service.replaceUserRoles(10L, List.of(1L))).isInstanceOf(InactiveRoleCannotBeUsedException.class);
        verifyNoInteractions(bindings);
    }
    @Test void duplicateAssignmentFailsWithoutAnotherSave() {
        existing(role(1, RoleKind.CUSTOM, RoleStatus.ACTIVE));
        when(bindings.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.assignRoleToUser(10L, 1L)).isInstanceOf(UserRoleAlreadyExistsException.class);
        verify(bindings, never()).save(any());
    }
    @Test void revokeCanRemoveAnInactiveRoleButRejectsAbsentBinding() {
        existing(role(1, RoleKind.CUSTOM, RoleStatus.INACTIVE));
        assertThatThrownBy(() -> service.revokeRoleFromUser(10L, 1L)).isInstanceOf(UserRoleNotFoundException.class);
        verify(bindings, never()).deleteByUserIdAndRoleId(any(), any());
        when(bindings.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
        service.revokeRoleFromUser(10L, 1L);
        verify(bindings).deleteByUserIdAndRoleId(10L, 1L);
    }
    @Test void replacementDeduplicatesRequestedRolesAndNeverRemovesRoot() {
        var requested = role(2, RoleKind.CUSTOM, RoleStatus.ACTIVE);
        existing(requested);
        when(bindings.findRolesByUserId(10L)).thenReturn(List.of(
                role(1, RoleKind.ROOT, RoleStatus.ACTIVE), role(3, RoleKind.CUSTOM, RoleStatus.INACTIVE)));
        service.replaceUserRoles(10L, List.of(2L, 2L));
        verify(bindings, never()).deleteByUserIdAndRoleId(10L, 1L);
        verify(bindings).deleteByUserIdAndRoleId(10L, 3L);
        verify(bindings).saveAll(argThat(all -> all.size() == 1 && all.get(0).getRoleId().equals(2L)
                && all.get(0).getUserId().equals(10L) && all.get(0).getCreatedAt().equals(NOW)));
    }
    @Test void emptyReplacementRemovesOnlyCustomBindingsWithoutSavingAnEmptyBatch() {
        when(users.existsByIdForUpdate(10L)).thenReturn(true);
        when(bindings.findRolesByUserId(10L)).thenReturn(List.of(
                role(1, RoleKind.ROOT, RoleStatus.ACTIVE), role(2, RoleKind.CUSTOM, RoleStatus.ACTIVE)));
        service.replaceUserRoles(10L, List.of());
        verify(bindings).deleteByUserIdAndRoleId(10L, 2L);
        verify(bindings, never()).deleteByUserIdAndRoleId(10L, 1L);
        verify(bindings, never()).saveAll(any());
    }
    @Test void nullReplacementIsRejectedBeforeReadingOrRemovingBindings() {
        when(users.existsByIdForUpdate(10L)).thenReturn(true);
        assertThatNullPointerException().isThrownBy(() -> service.replaceUserRoles(10L, null));
        verifyNoInteractions(bindings, roles);
    }
}
