package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.iam.authorization.core.application.service.role.AdminRoleService;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.domain.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.enums.*;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.api.module.spi.RoleDeletionGuard;
import com.jeepclub.backend.platform.logging.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminRoleServiceTest {
    static final Instant CREATED = Instant.parse("2026-10-01T12:00:00Z");
    static final Instant NOW = CREATED.plusSeconds(60);
    final RoleRepository roles = mock(RoleRepository.class);
    final RoleDeletionGuard guard = mock(RoleDeletionGuard.class);
    final SystemLogService audit = mock(SystemLogService.class);
    final AdminRoleService service = new AdminRoleService(roles, Clock.fixed(NOW, ZoneOffset.UTC), List.of(guard), audit);
    Role existing(RoleKind kind) {
        var role = Role.reconstitute(1L, "Name", "Description", kind, RoleStatus.ACTIVE, CREATED, CREATED, null);
        when(roles.findByIdForUpdate(1L)).thenReturn(Optional.of(role));
        return role;
    }
    @Test void normalizedDuplicateCreateOrUpdateNeverPersists() {
        when(roles.existsByName("Name")).thenReturn(true);
        assertThatThrownBy(() -> service.createRole(" Name ", null)).isInstanceOf(RoleAlreadyExistsException.class);
        var role = existing(RoleKind.CUSTOM);
        when(roles.existsByNameAndIdNot("Other", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateRole(1L, " Other ", "Changed")).isInstanceOf(RoleAlreadyExistsException.class);
        assertThat(role.getName()).isEqualTo("Name");
        verify(roles, never()).save(any());
    }
    @Test void createUsesCustomRoleAndInjectedClock() {
        when(roles.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.createRole(" Name ", " Description ").role();
        assertThat(result.getName()).isEqualTo("Name");
        assertThat(result.getDescription()).isEqualTo("Description");
        assertThat(result.getKind()).isEqualTo(RoleKind.CUSTOM);
        assertThat(result.getStatus()).isEqualTo(RoleStatus.ACTIVE);
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        verify(roles).save(result);
    }
    @Test void updateNoopDoesNotSaveAndChangedDataIsPersisted() {
        var role = existing(RoleKind.CUSTOM);
        assertThat(service.updateRole(1L, " Name ", " Description ").role()).isSameAs(role);
        verify(roles, never()).save(any());
        when(roles.save(role)).thenReturn(role);
        assertThat(service.updateRole(1L, "Changed", "Description").role().getName()).isEqualTo("Changed");
        assertThat(role.getUpdatedAt()).isEqualTo(NOW);
        verify(roles).save(role);
    }
    @Test void repeatedActivationAndDeactivationSaveAndAuditOnlyRealChanges() {
        var role = existing(RoleKind.CUSTOM);
        when(roles.save(role)).thenReturn(role);
        service.activateRole(1L, 9L);
        verify(roles, never()).save(any());
        verifyNoInteractions(audit);
        assertThat(service.deactivateRole(1L,9L).role().getStatus()).isEqualTo(RoleStatus.INACTIVE);
        verify(roles).save(role);
        service.deactivateRole(1L, 9L);
        verify(roles).save(role);
        verify(audit).recordInTransaction(argThat(e -> e.actorId().equals(9L)
                && e.action().equals("AUTHORIZATION_ROLE_DEACTIVATE:{ACTIVE}") && e.occurredAt().equals(NOW)));
        assertThat(service.activateRole(1L,9L).role().getStatus()).isEqualTo(RoleStatus.ACTIVE);
        service.activateRole(1L,9L);
        verify(roles,times(2)).save(role);
        verify(audit).recordInTransaction(argThat(e -> e.actorId().equals(9L)
                && e.action().equals("AUTHORIZATION_ROLE_ACTIVATE:{INACTIVE}") && e.occurredAt().equals(NOW)));
        verify(audit,times(2)).recordInTransaction(any());
    }
    @Test void archivalAndRestorationSaveAndAuditEachTransitionExactlyOnce() {
        var role=existing(RoleKind.CUSTOM);
        when(roles.save(role)).thenReturn(role);
        assertThat(service.archiveRole(1L,9L).role().getStatus()).isEqualTo(RoleStatus.ARCHIVED);
        assertThat(service.archiveRole(1L,9L).role().getArchivedAt()).isEqualTo(NOW);
        verify(roles).save(role);
        verify(audit).recordInTransaction(argThat(e -> e.action().equals("AUTHORIZATION_ROLE_ARCHIVE")
                && e.path().equals("/authorization/roles/1/archive") && e.actorId().equals(9L)));
        assertThat(service.unarchiveRole(1L,9L).role().getStatus()).isEqualTo(RoleStatus.INACTIVE);
        assertThat(service.unarchiveRole(1L,9L).role().getArchivedAt()).isNull();
        verify(roles,times(2)).save(role);
        verify(audit).recordInTransaction(argThat(e -> e.action().equals("AUTHORIZATION_ROLE_UNARCHIVE")
                && e.path().equals("/authorization/roles/1/unarchive") && e.actorId().equals(9L)));
        verify(audit,times(2)).recordInTransaction(any());
    }
    @Test void readQueriesPreserveTheRepositoryRoleResultsAndMissingIdsAreRejected() {
        var role=Role.reconstitute(1L,"Name",null,RoleKind.CUSTOM,RoleStatus.ACTIVE,CREATED,CREATED,null);
        when(roles.findAll()).thenReturn(List.of(role));
        when(roles.findById(1L)).thenReturn(Optional.of(role));
        assertThat(service.findAllRoles().roles()).containsExactly(role);
        assertThat(service.findRoleById(1L).role()).isSameAs(role);
        verify(roles).findAll();
        verify(roles).findById(1L);
        verifyNoInteractions(audit,guard);
    }
    @Test void missingRoleDoesNotReachMutationOrDeletionGuards() {
        assertThatThrownBy(() -> service.findRoleById(1L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.updateRole(1L, "Name", null)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.activateRole(1L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.deactivateRole(1L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.activateRole(1L,9L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.deactivateRole(1L,9L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.archiveRole(1L,9L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.unarchiveRole(1L,9L)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> service.deleteRole(1L, 9L)).isInstanceOf(RoleNotFoundException.class);
        verifyNoInteractions(guard, audit);
        verify(roles, never()).save(any());
    }
    @Test void deletionRequiresActorAndRejectsRootBeforeDependencyGuards() {
        assertThatNullPointerException().isThrownBy(() -> service.deleteRole(1L, null));
        verifyNoInteractions(roles);
        existing(RoleKind.ROOT);
        assertThatThrownBy(() -> service.deleteRole(1L, 9L)).isInstanceOf(RootRoleCannotBeChangedException.class);
        verifyNoInteractions(guard, audit);
        verify(roles, never()).deleteWithHistory(any(), any(), any());
    }
    @Test void retainedReferencesPreventDeletionAndAudit() {
        existing(RoleKind.CUSTOM);
        when(guard.hasRetainedReferences(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteRole(1L, 9L)).isInstanceOf(RoleLifecycleConflictException.class);
        verify(roles, never()).deleteWithHistory(any(), any(), any());
        verifyNoInteractions(audit);
    }
    @Test void successfulDeletionSnapshotsBeforeMandatoryAuditAndPropagatesAuditFailure() {
        var role = existing(RoleKind.CUSTOM);
        var failure = new IllegalStateException("Synthetic audit failure");
        doThrow(failure).when(audit).recordInTransaction(any());
        assertThatThrownBy(() -> service.deleteRole(1L, 9L)).isSameAs(failure);
        var order = inOrder(roles, audit);
        order.verify(roles).deleteWithHistory(1L, 9L, NOW);
        order.verify(audit).recordInTransaction(argThat(e -> e.actorId().equals(9L)
                && e.action().equals("AUTHORIZATION_ROLE_DELETE") && e.status() == 204));
        assertThat(role.getName()).isEqualTo("Name");
        // Transaction rollback is separately exercised against real persistence.
    }
}
