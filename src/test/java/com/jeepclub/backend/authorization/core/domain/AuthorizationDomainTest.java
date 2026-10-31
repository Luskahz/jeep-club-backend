package com.jeepclub.backend.authorization.core.domain;

import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.domain.enums.*;
import com.jeepclub.backend.iam.authorization.core.domain.exception.role.*;
import com.jeepclub.backend.iam.authorization.core.domain.exception.permission.*;
import com.jeepclub.backend.shared.authorization.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class AuthorizationDomainTest {
    static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t"})
    void absentRoleDescriptionIsCanonicalNull(String description) {
        assertThat(Role.create("Name",description,NOW).getDescription()).isNull();
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void archivalTransitionsRejectPastOrMissingTimeWithoutMutatingState(boolean restore) {
        var role=Role.create("Name",null,NOW);
        if(restore)role.archive(NOW);
        var status=role.getStatus();var archived=role.getArchivedAt();
        assertThatIllegalArgumentException().isThrownBy(() -> {
            if(restore)role.unarchive(NOW.minusNanos(1));else role.archive(NOW.minusNanos(1));
        });
        assertThatNullPointerException().isThrownBy(() -> {
            if(restore)role.unarchive(null);else role.archive(null);
        });
        assertThat(role.getStatus()).isEqualTo(status);
        assertThat(role.getArchivedAt()).isEqualTo(archived);
        assertThat(role.getUpdatedAt()).isEqualTo(NOW);
    }
    @Test void unarchivalRequiresArchivedStateAndIsIdempotentOnceInactive() {
        var role=Role.create("Name",null,NOW);
        assertThatThrownBy(() -> role.unarchive(NOW.plusSeconds(1))).isInstanceOf(RoleLifecycleConflictException.class);
        role.deactivate(NOW.plusSeconds(2));
        assertThat(role.unarchive(NOW.plusSeconds(3))).isFalse();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "\t"})
    void roleNameCannotBeAbsent(String name) {
        assertThatThrownBy(() -> Role.create(name, null, NOW)).isInstanceOf(RoleNameCannotBeBlankException.class);
    }
    @Test void roleNormalizesBoundedNameAndDescriptionAndRejectsOverflow() {
        var role = Role.create(" " + "N".repeat(100) + " ", " " + "D".repeat(255) + " ", NOW);
        assertThat(role.getName()).isEqualTo("N".repeat(100));
        assertThat(role.getDescription()).isEqualTo("D".repeat(255));
        assertThatThrownBy(() -> Role.create("N".repeat(101), null, NOW)).isInstanceOf(RoleNameTooLongException.class);
        assertThatThrownBy(() -> Role.create("Name", "D".repeat(256), NOW)).isInstanceOf(RoleDescriptionTooLongException.class);
    }
    @Test void roleNoopPreservesTimestampWhileNameOrDescriptionChangesAreDetected() {
        var role = Role.create("Name", "Description", NOW);
        assertThat(role.update(" Name ", " Description ", NOW.plusSeconds(1))).isFalse();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW);
        assertThat(role.update("Renamed", "Description", NOW.plusSeconds(2))).isTrue();
        assertThat(role.getName()).isEqualTo("Renamed");
        assertThat(role.update("Renamed", "Changed", NOW.plusSeconds(3))).isTrue();
        assertThat(role.getDescription()).isEqualTo("Changed");
        assertThat(role.update("Renamed", " ", NOW.plusSeconds(4))).isTrue();
        assertThat(role.getDescription()).isNull();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW.plusSeconds(4));
    }
    @Test void repeatedActivationAndDeactivationDoNotRewriteTransitionTime() {
        var role = Role.create("Name", null, NOW);
        assertThat(role.activate(NOW.plusSeconds(1))).isFalse();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW);
        assertThat(role.deactivate(NOW.plusSeconds(2))).isTrue();
        assertThat(role.isActive()).isFalse();
        assertThat(role.deactivate(NOW.plusSeconds(3))).isFalse();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(role.activate(NOW.plusSeconds(4))).isTrue();
        assertThat(role.isActive()).isTrue();
        assertThat(role.getUpdatedAt()).isEqualTo(NOW.plusSeconds(4));
    }
    @Test void reconstitutionRejectsInactiveRootAndInconsistentArchivalMetadata() {
        assertThatIllegalStateException().isThrownBy(() -> Role.reconstitute(1L, "ROOT", null,
                RoleKind.ROOT, RoleStatus.INACTIVE, NOW, NOW, null));
        assertThatIllegalArgumentException().isThrownBy(() -> Role.reconstitute(1L, "Role", null,
                RoleKind.CUSTOM, RoleStatus.ARCHIVED, NOW, NOW, null));
        assertThatIllegalArgumentException().isThrownBy(() -> Role.reconstitute(1L, "Role", null,
                RoleKind.CUSTOM, RoleStatus.ACTIVE, NOW, NOW, null, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> Role.reconstitute(1L, "Role", null,
                RoleKind.CUSTOM, RoleStatus.ARCHIVED, NOW, NOW, null, NOW.minusSeconds(1)));
    }
    @Test void permissionSynchronizationChangesOnlySemanticMetadataAndIsIdempotent() {
        var definition = PermissionDefinition.AUTHORIZATION_ROLE_READ;
        var permission = Permission.reconstitute(5L, definition.getCode(), "Old", ModuleCode.IDENTITY, NOW, NOW);
        assertThat(permission.synchronizeWith(definition, NOW.plusSeconds(1))).isTrue();
        assertThat(permission.getDescription()).isEqualTo(definition.getDescription());
        assertThat(permission.getModule()).isEqualTo(definition.getModule());
        assertThat(permission.getId()).isEqualTo(5L);
        assertThat(permission.getCreatedAt()).isEqualTo(NOW);
        assertThat(permission.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(permission.synchronizeWith(definition, NOW.plusSeconds(2))).isFalse();
        assertThat(permission.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }
    @Test void permissionSynchronizationDetectsDescriptionAndModuleIndependently() {
        var definition = PermissionDefinition.AUTHORIZATION_ROLE_READ;
        var description = Permission.reconstitute(5L, definition.getCode(), "Old", definition.getModule(), NOW, NOW);
        var module = Permission.reconstitute(6L, definition.getCode(), definition.getDescription(), ModuleCode.IDENTITY, NOW, NOW);
        assertThat(description.synchronizeWith(definition, NOW.plusSeconds(1))).isTrue();
        assertThat(description.getDescription()).isEqualTo(definition.getDescription());
        assertThat(module.synchronizeWith(definition, NOW.plusSeconds(1))).isTrue();
        assertThat(module.getModule()).isEqualTo(definition.getModule());
    }
    @Test void mismatchedPermissionCodeCannotRewriteMetadata() {
        var permission = Permission.fromDefinition(PermissionDefinition.AUTHORIZATION_ROLE_READ, NOW);
        assertThatThrownBy(() -> permission.synchronizeWith(PermissionDefinition.IDENTITY_USER_READ, NOW.plusSeconds(1)))
                .isInstanceOf(PermissionCodeMismatchException.class);
        assertThat(permission.getCode()).isEqualTo(PermissionCode.AUTHORIZATION_ROLE_READ);
        assertThat(permission.getUpdatedAt()).isEqualTo(NOW);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "\t"})
    void permissionDescriptionCannotBeAbsent(String description) {
        assertThatThrownBy(() -> Permission.reconstitute(5L, PermissionCode.AUTHORIZATION_ROLE_READ,
                description, ModuleCode.AUTHORIZATION, NOW, NOW)).isInstanceOf(PermissionDescriptionCannotBeBlankException.class);
    }
    @Test void permissionDescriptionHasAnInclusive255CharacterBoundary() {
        assertThat(Permission.reconstitute(5L, PermissionCode.AUTHORIZATION_ROLE_READ,
                " " + "D".repeat(255) + " ", ModuleCode.AUTHORIZATION, NOW, NOW).getDescription()).isEqualTo("D".repeat(255));
        assertThatThrownBy(() -> Permission.reconstitute(5L, PermissionCode.AUTHORIZATION_ROLE_READ,
                "D".repeat(256), ModuleCode.AUTHORIZATION, NOW, NOW)).isInstanceOf(PermissionDescriptionTooLongException.class);
    }
    @ParameterizedTest @ValueSource(longs={0,-1})
    void bindingIdentifiersMustBePositive(long invalid) {
        assertThatIllegalArgumentException().isThrownBy(() -> UserRole.create(invalid, 1L, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> UserRole.create(1L, invalid, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> RolePermission.create(invalid, 1L, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> RolePermission.create(1L, invalid, NOW));
    }
}
