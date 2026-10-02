package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependentTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Test
    void createsActiveDependentWithNormalizedData() {
        Dependent dependent = create();

        assertThat(dependent.getName()).isEqualTo("João Silva");
        assertThat(dependent.getCpf()).isEqualTo("12345678900");
        assertThat(dependent.getPhoneNumber()).isEqualTo("11999999999");
        assertThat(dependent.getStatus()).isEqualTo(DependentStatus.ACTIVE);
        assertThat(dependent.getCreatedAt()).isEqualTo(NOW);
        assertThat(dependent.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsMissingName(String name) {
        assertThatThrownBy(() -> Dependent.create(
                name, "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1234567890", "123456789012"})
    void rejectsMissingOrInvalidCpf(String cpf) {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", cpf, LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "123456789012", "phone"})
    void rejectsInvalidPhoneNumber(String phoneNumber) {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, phoneNumber, 1L, NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phoneNumber must contain 10 or 11 digits.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void acceptsMissingPhoneNumber(String phoneNumber) {
        Dependent dependent = Dependent.create(
                "João Silva", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, phoneNumber, 1L, NOW
        );

        assertThat(dependent.getPhoneNumber()).isNull();
    }

    @Test
    void rejectsMissingBirthDate() {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", "12345678900", null,
                RelationshipType.CHILD, null, 1L, NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("birthDate cannot be null");
    }

    @Test
    void rejectsMissingRelationshipType() {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", "12345678900", LocalDate.of(2015, 5, 10),
                null, null, 1L, NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("relationshipType cannot be null");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    void rejectsNonPositiveUserId(long userId) {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, userId, NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must be positive.");
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> Dependent.create(
                "João Silva", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, null, NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must be positive.");
    }

    @Test
    void updatesActiveDependent() {
        Dependent dependent = create();
        Instant updatedAt = NOW.plusSeconds(60);

        dependent.update(
                "Maria Silva",
                "987.654.321-00",
                LocalDate.of(2014, 1, 2),
                RelationshipType.CHILD,
                "(11) 98888-7777",
                updatedAt
        );

        assertThat(dependent.getName()).isEqualTo("Maria Silva");
        assertThat(dependent.getCpf()).isEqualTo("98765432100");
        assertThat(dependent.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void blocksUpdateWhenDisabled() {
        Dependent dependent = create();
        dependent.disable(NOW.plusSeconds(30));

        assertThatThrownBy(() -> dependent.update(
                "Maria Silva",
                "98765432100",
                LocalDate.of(2014, 1, 2),
                RelationshipType.CHILD,
                null,
                NOW.plusSeconds(60)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void disablesAndEnablesDependent() {
        Dependent dependent = create();

        dependent.disable(NOW.plusSeconds(30));
        assertThat(dependent.getStatus()).isEqualTo(DependentStatus.DISABLED);

        dependent.enable(NOW.plusSeconds(60));
        assertThat(dependent.getStatus()).isEqualTo(DependentStatus.ACTIVE);
        assertThat(dependent.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    private Dependent create() {
        return Dependent.create(
                "  João Silva  ",
                "123.456.789-00",
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "(11) 99999-9999",
                1L,
                NOW
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "disable", "enable"})
    void rejectsMutationBeforeCreationWithoutChangingState(String operation) {
        Dependent dependent = create();
        if (operation.equals("enable")) dependent.disable(NOW);
        DependentStatus originalStatus = dependent.getStatus();
        Instant originalUpdatedAt = dependent.getUpdatedAt();

        assertThatThrownBy(() -> mutate(dependent, operation, NOW.minusNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dependent.getStatus()).isEqualTo(originalStatus);
        assertThat(dependent.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        assertThat(dependent.getName()).isEqualTo("João Silva");
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "disable", "enable"})
    void acceptsMutationAtCreationTime(String operation) {
        Dependent dependent = create();
        if (operation.equals("enable")) dependent.disable(NOW);
        mutate(dependent, operation, NOW);
        assertThat(dependent.getUpdatedAt()).isEqualTo(dependent.getCreatedAt());
    }

    @Test
    void reconstitutionAllowsUnchangedOrEqualTimestampsButRejectsInversion() {
        assertThat(reconstitute(null).getUpdatedAt()).isNull();
        assertThat(reconstitute(NOW).getUpdatedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> reconstitute(NOW.minusNanos(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void repeatedStatusTransitionsPreserveLastChangeTime() {
        Dependent dependent = create();
        dependent.enable(NOW.plusSeconds(1));
        assertThat(dependent.getUpdatedAt()).isNull();
        dependent.disable(NOW.plusSeconds(2));
        dependent.disable(NOW.plusSeconds(3));
        assertThat(dependent.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        dependent.enable(NOW.plusSeconds(4));
        dependent.enable(NOW.plusSeconds(5));
        assertThat(dependent.getUpdatedAt()).isEqualTo(NOW.plusSeconds(4));
    }

    @Test
    void acceptsTenDigitPhoneAndNormalizesUpdateFields() {
        Dependent dependent = create();
        dependent.update(" Maria ", "987.654.321-00", LocalDate.of(2014, 1, 2),
                RelationshipType.SIBLING, "(11) 3333-4444", NOW);
        assertThat(dependent.getName()).isEqualTo("Maria");
        assertThat(dependent.getBirthDate()).isEqualTo(LocalDate.of(2014, 1, 2));
        assertThat(dependent.getRelationshipType()).isEqualTo(RelationshipType.SIBLING);
        assertThat(dependent.getPhoneNumber()).isEqualTo("1133334444");
        assertThat(dependent.getUserId()).isEqualTo(1L);
        assertThat(dependent.getCreatedAt()).isEqualTo(NOW);
    }

    private Dependent reconstitute(Instant updatedAt) {
        return Dependent.reconstitute(1L, "João", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, DependentStatus.ACTIVE, NOW, updatedAt);
    }

    @Test
    void creationRequiresExplicitTime() {
        assertThatThrownBy(() -> Dependent.create("João", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstitutionRejectsMissingNameAndInvalidOwner() {
        assertThatThrownBy(() -> Dependent.reconstitute(1L, " ", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, DependentStatus.ACTIVE, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dependent.reconstitute(1L, "João", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 0L, DependentStatus.ACTIVE, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRejectsBlankNameWithoutChangingExistingData() {
        Dependent dependent = create();
        assertThatThrownBy(() -> dependent.update(" ", "98765432100", LocalDate.of(2014, 1, 2),
                RelationshipType.CHILD, null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(dependent.getName()).isEqualTo("João Silva");
        assertThat(dependent.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void reconstitutionRejectsInvalidIdentity(Long id) {
        assertThatThrownBy(() -> Dependent.reconstitute(id, "João", "12345678900", LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD, null, 1L, DependentStatus.ACTIVE, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "disable", "enable"})
    void mutationsRequireExplicitTime(String operation) {
        assertThatThrownBy(() -> mutate(create(), operation, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private void mutate(Dependent dependent, String operation, Instant time) {
        switch (operation) {
            case "update" -> dependent.update("Maria", "98765432100", LocalDate.of(2014, 1, 2),
                    RelationshipType.CHILD, null, time);
            case "disable" -> dependent.disable(time);
            case "enable" -> dependent.enable(time);
            default -> throw new AssertionError(operation);
        }
    }
}
