package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;

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
}
