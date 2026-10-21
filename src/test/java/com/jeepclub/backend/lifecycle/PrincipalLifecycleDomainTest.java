package com.jeepclub.backend.lifecycle;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.domain.enums.*;
import com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolLifecycleConflictException;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.domain.enums.*;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException;
import org.junit.jupiter.api.Test;

import java.time.*;
import static org.assertj.core.api.Assertions.*;

class PrincipalLifecycleDomainTest {
    private static final Instant CREATED = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant ARCHIVED = CREATED.plusSeconds(60);
    private static final Instant RESTORED = ARCHIVED.plusSeconds(60);
    private User user() {
        return User.create("Synthetic user", null, null, "00000000000", null, null, null, CREATED);
    }
    private Dependent dependent() {
        return Dependent.create("Synthetic dependent", "00000000000", LocalDate.of(2010, 1, 1),
                RelationshipType.CHILD, null, 1L, CREATED);
    }
    private Tool tool() {
        return Tool.create("Synthetic tool", null, 1L, local(CREATED));
    }
    private LocalDateTime local(Instant time) { return LocalDateTime.ofInstant(time, ZoneOffset.UTC); }
    private Vehicle vehicle() {
        return Vehicle.create("Synthetic vehicle", null, "ABC1D23", "00000000000", "Jeep", "Test",
                2020, 2020, "Blue", 5, FuelType.DIESEL, 2, false, 1L, CREATED);
    }

    @Test void usersArchiveAndUnarchiveRequireSeparateEnableAndPreserveOriginalSuspension() {
        var user = user();
        user.disable(CREATED.plusSeconds(10));
        assertThat(user.archive(ARCHIVED)).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ARCHIVED);
        assertThat(user.getDisabledAt()).isEqualTo(CREATED.plusSeconds(10));
        assertThatThrownBy(() -> user.enable(RESTORED)).isInstanceOf(
                com.jeepclub.backend.iam.identity.core.domain.exception.UserNotDisabledException.class);
        assertThat(user.unarchive(RESTORED)).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.isActive()).isFalse();
        user.enable(RESTORED.plusSeconds(1));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDisabledAt()).isNull();
        assertThat(user.getArchivedAt()).isNull();
    }

    @Test void userArchiveIsIdempotentAndCannotBeBypassedByProfileOrDisable() {
        var user = user();
        user.archive(ARCHIVED);
        assertThat(user.archive(RESTORED)).isFalse();
        assertThat(user.getArchivedAt()).isEqualTo(ARCHIVED);
        assertThat(user.getUpdatedAt()).isEqualTo(ARCHIVED);
        assertThatThrownBy(() -> user.disable(RESTORED)).isInstanceOf(UserLifecycleConflictException.class);
        assertThatThrownBy(() -> user.updateEmail("new@example.invalid", RESTORED))
                .isInstanceOf(UserLifecycleConflictException.class);
        assertThat(user.getEmail()).isNull();
    }

    @Test void activeUserCannotBeUnarchivedAndArchivalBeforeCreationIsRejected() {
        var user = user();
        assertThatThrownBy(() -> user.unarchive(ARCHIVED)).isInstanceOf(UserLifecycleConflictException.class);
        assertThatThrownBy(() -> user.archive(CREATED.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test void dependentArchivePreservesIdentityAndRequiresSeparateEnable() {
        var dependent = dependent();
        assertThat(dependent.archive(ARCHIVED)).isTrue();
        assertThat(dependent.getStatus()).isEqualTo(DependentStatus.ARCHIVED);
        assertThat(dependent.archive(RESTORED)).isFalse();
        assertThat(dependent.getUpdatedAt()).isEqualTo(ARCHIVED);
        assertThatThrownBy(() -> dependent.enable(RESTORED)).isInstanceOf(DependentLifecycleConflictException.class);
        assertThatThrownBy(() -> dependent.disable(RESTORED)).isInstanceOf(DependentLifecycleConflictException.class);
        dependent.unarchive(RESTORED);
        assertThat(dependent.getStatus()).isEqualTo(DependentStatus.INACTIVE);
        assertThat(dependent.getUserId()).isEqualTo(1L);
        assertThat(dependent.unarchive(RESTORED.plusSeconds(1))).isFalse();
        dependent.enable(RESTORED.plusSeconds(2));
        assertThat(dependent.isActive()).isTrue();
    }

    @Test void dependentInactiveCanBeArchivedButArchivedCannotBeEdited() {
        var dependent = dependent();
        dependent.disable(ARCHIVED);
        dependent.archive(RESTORED);
        assertThatThrownBy(() -> dependent.update("Changed", "00000000000", LocalDate.of(2010, 1, 1),
                RelationshipType.CHILD, null, RESTORED)).isInstanceOf(IllegalStateException.class);
        assertThat(dependent.getName()).isEqualTo("Synthetic dependent");
    }

    @Test void toolsArchiveAndUnarchiveRetainDetailsAndRequireExplicitActivation() {
        var tool = tool();
        tool.deactivate(local(ARCHIVED));
        assertThat(tool.archive(local(RESTORED))).isTrue();
        assertThat(tool.archive(local(RESTORED.plusSeconds(1)))).isFalse();
        assertThatThrownBy(() -> tool.activate(local(RESTORED))).isInstanceOf(ToolLifecycleConflictException.class);
        assertThatThrownBy(() -> tool.deactivate(local(RESTORED))).isInstanceOf(ToolLifecycleConflictException.class);
        assertThatThrownBy(() -> tool.updateDetails("Changed", null, local(RESTORED)))
                .isInstanceOf(ToolLifecycleConflictException.class);
        tool.unarchive(local(RESTORED.plusSeconds(2)));
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.INACTIVE);
        assertThat(tool.getName()).isEqualTo("Synthetic tool");
        tool.activate(local(RESTORED.plusSeconds(3)));
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.ACTIVE);
    }

    @Test void vehicleSupportsAllAvailabilityStatesWithoutDeletingItsPhoto() {
        var vehicle = vehicle();
        assertThat(vehicle.deactivate(ARCHIVED)).isTrue();
        assertThat(vehicle.deactivate(RESTORED)).isFalse();
        vehicle.archive(RESTORED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ARCHIVED);
        assertThatThrownBy(() -> vehicle.activate(RESTORED)).isInstanceOf(VehicleLifecycleConflictException.class);
        assertThatThrownBy(() -> vehicle.deactivate(RESTORED)).isInstanceOf(VehicleLifecycleConflictException.class);
        vehicle.unarchive(RESTORED.plusSeconds(1));
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
        vehicle.activate(RESTORED.plusSeconds(2));
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getPlate()).isEqualTo("ABC1D23");
    }

    @Test void historicallyDeletedVehicleCannotBeResurrectedByUnarchive() {
        var vehicle = Vehicle.reconstitute(5L, "Legacy", "https://legacy.invalid/photo.jpg", "ABC1D23",
                "00000000000", "Jeep", "Test", 2020, 2020, "Blue", 5, FuelType.DIESEL, 2,
                VehicleStatus.ARCHIVED, false, 1L, CREATED, ARCHIVED, ARCHIVED, true);
        assertThatThrownBy(() -> vehicle.unarchive(RESTORED)).isInstanceOf(VehicleLifecycleConflictException.class);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ARCHIVED);
        assertThat(vehicle.getPhoto()).isEqualTo("https://legacy.invalid/photo.jpg");
        assertThat(vehicle.isLegacyDeleted()).isTrue();
    }

    @Test void archiveTimestampMustMatchPersistedAvailabilityState() {
        assertThatThrownBy(() -> Tool.reconstitute(1L, "Tool", null, ToolStatus.ARCHIVED, 1L,
                local(CREATED), local(ARCHIVED))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dependent.reconstitute(1L, "Dependent", "00000000000", LocalDate.of(2010, 1, 1),
                RelationshipType.CHILD, null, 1L, DependentStatus.ACTIVE, CREATED, ARCHIVED, ARCHIVED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void roleArchiveRevokesEligibilityAndRequiresExplicitActivationAfterUnarchive() {
        var role = com.jeepclub.backend.iam.authorization.core.domain.model.Role.create("Custom", null, CREATED);
        assertThat(role.archive(ARCHIVED)).isTrue();
        assertThat(role.archive(RESTORED)).isFalse();
        assertThat(role.isActive()).isFalse();
        assertThat(role.getArchivedAt()).isEqualTo(ARCHIVED);
        assertThatThrownBy(role::ensureActive).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.InactiveRoleCannotBeUsedException.class);
        assertThatThrownBy(() -> role.activate(RESTORED)).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RoleLifecycleConflictException.class);
        assertThatThrownBy(() -> role.update("Changed", null, RESTORED)).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RoleLifecycleConflictException.class);
        assertThatCode(role::ensureCanBeDeleted).doesNotThrowAnyException();
        assertThat(role.unarchive(RESTORED)).isTrue();
        assertThat(role.getStatus()).isEqualTo(com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.INACTIVE);
        assertThat(role.isActive()).isFalse();
        assertThat(role.activate(RESTORED.plusSeconds(1))).isTrue();
        assertThat(role.isActive()).isTrue();
    }

    @Test void rootCannotBeArchivedUnarchivedDeactivatedOrDeleted() {
        var root = com.jeepclub.backend.iam.authorization.core.domain.model.Role.createRoot("ROOT", null, CREATED);
        assertThatThrownBy(() -> root.archive(ARCHIVED)).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RootRoleCannotBeChangedException.class);
        assertThatThrownBy(() -> root.unarchive(ARCHIVED)).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RootRoleCannotBeChangedException.class);
        assertThatThrownBy(() -> root.deactivate(ARCHIVED)).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RootRoleCannotBeChangedException.class);
        assertThatThrownBy(root::ensureCanBeDeleted).isInstanceOf(
                com.jeepclub.backend.iam.authorization.core.domain.exception.role.RootRoleCannotBeChangedException.class);
        assertThat(root.isActive()).isTrue();
    }

    @Test void operationalRoleCannotReconstituteAnOldDeletionTimestamp() {
        assertThatThrownBy(() -> com.jeepclub.backend.iam.authorization.core.domain.model.Role.reconstitute(
                1L, "Old", null, com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind.CUSTOM,
                com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.INACTIVE, CREATED, ARCHIVED, ARCHIVED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void allSixAvailabilityEnumsExposeExactlyThreeStatesAndNoDeletionState() {
        java.util.List<Enum<?>[]> enums = java.util.List.of(
                UserStatus.values(), DependentStatus.values(), ToolStatus.values(), VehicleStatus.values(),
                com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.values(),
                com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus.values());
        enums.forEach(values -> assertThat(java.util.Arrays.stream(values).map(Enum::name))
                .containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "ARCHIVED"));
    }
}
