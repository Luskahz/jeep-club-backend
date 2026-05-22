package com.jeepclub.backend.membershipKauan.core.domain.model;

import com.jeepclub.backend.membershipKauan.core.domain.enums.RelationshipType;
import com.jeepclub.backend.membershipKauan.core.domain.exception.DependentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DependentTest {

    private final Instant now = Instant.now();

    @Test
    @DisplayName("Sucesso: Criar dependente com dados válidos e consentimento LGPD aceito")
    void shouldCreateDependentSuccessfully() {
        Dependent dependent = Dependent.create(
                "João Silva",
                "123.456.789-00",
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "11999999999",
                null,
                true,
                1L,
                now
        );

        assertNotNull(dependent);
        assertEquals("João Silva", dependent.getName());
        assertEquals("12345678900", dependent.getCpf()); // CPF normalizado
        assertEquals(LocalDate.of(2015, 5, 10), dependent.getBirthDate());
        assertEquals(RelationshipType.CHILD, dependent.getRelationshipType());
        assertEquals("11999999999", dependent.getPhoneNumber());
        assertTrue(dependent.isConsentAccepted());
        assertEquals(1L, dependent.getSocioId());
        assertEquals(now, dependent.getConsentAcceptedAt());
    }

    @Test
    @DisplayName("Falha: Criar dependente sem consentimento LGPD deve lançar DependentException")
    void shouldThrowExceptionWhenConsentNotAcceptedOnCreation() {
        DependentException exception = assertThrows(DependentException.class, () ->
                Dependent.create(
                        "João Silva",
                        "123.456.789-00",
                        LocalDate.of(2015, 5, 10),
                        RelationshipType.CHILD,
                        "11999999999",
                        null,
                        false,
                        1L,
                        now
                )
        );

        assertEquals("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.", exception.getMessage());
    }

    @Test
    @DisplayName("Falha: CPF é obrigatório")
    void shouldThrowExceptionWhenCpfIsNullOrEmpty() {
        DependentException exception = assertThrows(DependentException.class, () ->
                Dependent.create(
                        "Maria Silva",
                        null,
                        LocalDate.of(2018, 1, 1),
                        RelationshipType.CHILD,
                        "",
                        null,
                        true,
                        1L,
                        now
                )
        );
        assertEquals("CPF é obrigatório.", exception.getMessage());
    }

    @Test
    @DisplayName("Falha: CPF com número incorreto de dígitos deve lançar DependentException")
    void shouldThrowExceptionForInvalidCpfLength() {
        assertThrows(DependentException.class, () ->
                Dependent.create(
                        "João Silva",
                        "123456",
                        LocalDate.of(2015, 5, 10),
                        RelationshipType.CHILD,
                        "11999999999",
                        null,
                        true,
                        1L,
                        now
                )
        );
    }

    @Test
    @DisplayName("Sucesso: Atualizar dependente mantendo consentimento")
    void shouldUpdateDependentSuccessfully() {
        Dependent dependent = Dependent.create(
                "João Silva",
                "12345678900",
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "11999999999",
                null,
                true,
                1L,
                now
        );

        Instant updateTime = Instant.now();
        dependent.update(
                "João Silva Ramos",
                "98765432100",
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "11988888888",
                null,
                true,
                updateTime
        );

        assertEquals("João Silva Ramos", dependent.getName());
        assertEquals("98765432100", dependent.getCpf());
        assertEquals("11988888888", dependent.getPhoneNumber());
        assertEquals(updateTime, dependent.getUpdatedAt());
    }

    @Test
    @DisplayName("Falha: Atualizar dependente negando consentimento LGPD deve lançar DependentException")
    void shouldThrowExceptionWhenConsentRevokedOnUpdate() {
        Dependent dependent = Dependent.create(
                "João Silva",
                "12345678900",
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "11999999999",
                null,
                true,
                1L,
                now
        );

        DependentException exception = assertThrows(DependentException.class, () ->
                dependent.update(
                        "João Silva",
                        "12345678900",
                        LocalDate.of(2015, 5, 10),
                        RelationshipType.CHILD,
                        "11999999999",
                        null,
                        false,
                        Instant.now()
                )
        );

        assertEquals("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.", exception.getMessage());
    }
}
