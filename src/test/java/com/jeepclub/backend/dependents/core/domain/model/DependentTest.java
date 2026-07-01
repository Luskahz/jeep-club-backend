package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependentTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        void createsDependentWithNormalizedCpfPhoneAndConsentTimestamp() {
            Dependent dependent = Dependent.create(
                    "  João Silva  ",
                    "123.456.789-00",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "(11) 99999-9999",
                    true,
                    1L,
                    NOW
            );

            assertThat(dependent.getName()).isEqualTo("João Silva");
            assertThat(dependent.getCpf()).isEqualTo("12345678900");
            assertThat(dependent.getPhoneNumber()).isEqualTo("11999999999");
            assertThat(dependent.getRelationshipType()).isEqualTo(RelationshipType.CHILD);
            assertThat(dependent.isConsentAccepted()).isTrue();
            assertThat(dependent.getConsentAcceptedAt()).isEqualTo(NOW);
            assertThat(dependent.getSocioId()).isEqualTo(1L);
            assertThat(dependent.getCreatedAt()).isEqualTo(NOW);
            assertThat(dependent.getUpdatedAt()).isEqualTo(NOW);
        }

        @ParameterizedTest
        @CsvSource({
                "12345678900,12345678900",
                "123.456.789-00,12345678900",
                " 123.456.789-00 ,12345678900"
        })
        void acceptsExactlyElevenCpfDigitsAfterNormalization(String rawCpf, String expectedCpf) {
            Dependent dependent = validDependent(rawCpf);

            assertThat(dependent.getCpf()).isEqualTo(expectedCpf);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "1234567890", "123456789012"})
        void rejectsEmptyShortAndLongCpf(String cpf) {
            assertThatThrownBy(() -> validDependent(cpf))
                    .isInstanceOf(DependentException.class)
                    .hasMessageContaining(cpf == null || cpf.isBlank()
                            ? "CPF é obrigatório."
                            : "CPF deve conter exatamente 11 dígitos numéricos.");
        }

        @ParameterizedTest
        @CsvSource(value = {
                "null|null",
                "' '|null",
                "'(11) 98888-7777'|11988887777",
                "'11988887777'|11988887777"
        }, delimiter = '|', nullValues = "null")
        void normalizesOptionalPhone(String rawPhone, String expectedPhone) {
            Dependent dependent = Dependent.create(
                    "Maria Silva",
                    "98765432100",
                    LocalDate.of(2018, 1, 1),
                    RelationshipType.CHILD,
                    rawPhone,
                    true,
                    1L,
                    NOW
            );

            assertThat(dependent.getPhoneNumber()).isEqualTo(expectedPhone);
        }

        @Test
        void rejectsFalseConsent() {
            assertThatThrownBy(() -> Dependent.create(
                    "João Silva",
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "11999999999",
                    false,
                    1L,
                    NOW
            ))
                    .isInstanceOf(DependentException.class)
                    .hasMessage("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void rejectsBlankName(String name) {
            assertThatThrownBy(() -> Dependent.create(
                    name,
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "11999999999",
                    true,
                    1L,
                    NOW
            ))
                    .isInstanceOf(DependentException.class)
                    .hasMessage("Nome do dependente é obrigatório.");
        }

        @Test
        void rejectsMissingRelationshipTypeAndSocio() {
            assertThatThrownBy(() -> Dependent.create(
                    "João Silva",
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    null,
                    "11999999999",
                    true,
                    1L,
                    NOW
            ))
                    .isInstanceOf(DependentException.class)
                    .hasMessage("Tipo de parentesco é obrigatório.");

            assertThatThrownBy(() -> Dependent.create(
                    "João Silva",
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "11999999999",
                    true,
                    null,
                    NOW
            ))
                    .isInstanceOf(DependentException.class)
                    .hasMessage("ID do Sócio é obrigatório.");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void updatesDataAndKeepsOriginalConsentDateWhenAlreadyAccepted() {
            Dependent dependent = validDependent("12345678900");
            Instant updateTime = NOW.plusSeconds(3600);

            dependent.update(
                    "João Silva Ramos",
                    "987.654.321-00",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "(11) 98888-8888",
                    true,
                    updateTime
            );

            assertThat(dependent.getName()).isEqualTo("João Silva Ramos");
            assertThat(dependent.getCpf()).isEqualTo("98765432100");
            assertThat(dependent.getPhoneNumber()).isEqualTo("11988888888");
            assertThat(dependent.getConsentAcceptedAt()).isEqualTo(NOW);
            assertThat(dependent.getUpdatedAt()).isEqualTo(updateTime);
        }

        @Test
        void rejectsConsentRevocationOnUpdate() {
            Dependent dependent = validDependent("12345678900");

            assertThatThrownBy(() -> dependent.update(
                    "João Silva",
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "11999999999",
                    false,
                    NOW.plusSeconds(60)
            ))
                    .isInstanceOf(DependentException.class)
                    .hasMessage("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.");
        }

        @Test
        void acceptsRepeatedConsentWithoutChangingAcceptedAt() {
            Dependent dependent = validDependent("12345678900");
            Instant updateTime = NOW.plusSeconds(60);

            dependent.update(
                    "João Silva",
                    "12345678900",
                    LocalDate.of(2015, 5, 10),
                    RelationshipType.CHILD,
                    "11999999999",
                    true,
                    updateTime
            );

            assertThat(dependent.isConsentAccepted()).isTrue();
            assertThat(dependent.getConsentAcceptedAt()).isEqualTo(NOW);
            assertThat(dependent.getUpdatedAt()).isEqualTo(updateTime);
        }
    }

    private Dependent validDependent(String cpf) {
        return Dependent.create(
                "João Silva",
                cpf,
                LocalDate.of(2015, 5, 10),
                RelationshipType.CHILD,
                "11999999999",
                true,
                1L,
                NOW
        );
    }
}
