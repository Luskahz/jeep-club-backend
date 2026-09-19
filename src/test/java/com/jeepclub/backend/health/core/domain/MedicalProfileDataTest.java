package com.jeepclub.backend.health.core.domain;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import com.jeepclub.backend.health.core.domain.model.MedicalProfileData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicalProfileDataTest {

    @Test
    void normalizesEveryTextFieldAndFormattedPhone() {
        MedicalProfileData data = data(
                "  alergia  ",
                "  crônica  ",
                "  medicamento  ",
                "  operadora  ",
                "  plano  ",
                "  número  ",
                "  contato  ",
                "  (12) 99999-9999  ",
                "  parentesco  ",
                "  observação  "
        );

        assertThat(data.bloodType()).isEqualTo(BloodType.UNKNOWN);
        assertThat(data.allergies()).isEqualTo("alergia");
        assertThat(data.chronicConditions()).isEqualTo("crônica");
        assertThat(data.continuousMedications()).isEqualTo("medicamento");
        assertThat(data.healthInsuranceProvider()).isEqualTo("operadora");
        assertThat(data.healthInsurancePlan()).isEqualTo("plano");
        assertThat(data.healthInsuranceNumber()).isEqualTo("número");
        assertThat(data.emergencyContactName()).isEqualTo("contato");
        assertThat(data.emergencyContactPhone()).isEqualTo("12999999999");
        assertThat(data.emergencyContactRelationship()).isEqualTo("parentesco");
        assertThat(data.observations()).isEqualTo("observação");
    }

    @Test
    void convertsNullAndBlankValuesToNull() {
        MedicalProfileData nulls = data(
                null, null, null, null, null,
                null, null, null, null, null
        );
        MedicalProfileData blanks = data(
                "  ", "\t", "\n", "  ", "\t",
                "\n", "  ", "\t", "\n", "  "
        );

        assertThat(nulls.allergies()).isNull();
        assertThat(nulls.chronicConditions()).isNull();
        assertThat(nulls.continuousMedications()).isNull();
        assertThat(nulls.healthInsuranceProvider()).isNull();
        assertThat(nulls.healthInsurancePlan()).isNull();
        assertThat(nulls.healthInsuranceNumber()).isNull();
        assertThat(nulls.emergencyContactName()).isNull();
        assertThat(nulls.emergencyContactPhone()).isNull();
        assertThat(nulls.emergencyContactRelationship()).isNull();
        assertThat(nulls.observations()).isNull();
        assertThat(blanks.allergies()).isNull();
        assertThat(blanks.chronicConditions()).isNull();
        assertThat(blanks.continuousMedications()).isNull();
        assertThat(blanks.healthInsuranceProvider()).isNull();
        assertThat(blanks.healthInsurancePlan()).isNull();
        assertThat(blanks.healthInsuranceNumber()).isNull();
        assertThat(blanks.emergencyContactName()).isNull();
        assertThat(blanks.emergencyContactPhone()).isNull();
        assertThat(blanks.emergencyContactRelationship()).isNull();
        assertThat(blanks.observations()).isNull();
    }

    @Test
    void acceptsEveryTextFieldAtItsExactLimit() {
        MedicalProfileData data = data(
                "a".repeat(2000),
                "c".repeat(2000),
                "m".repeat(2000),
                "p".repeat(120),
                "l".repeat(120),
                "n".repeat(80),
                "e".repeat(120),
                "12999999999",
                "r".repeat(80),
                "o".repeat(2000)
        );

        assertThat(data.allergies()).hasSize(2000);
        assertThat(data.healthInsuranceProvider()).hasSize(120);
        assertThat(data.healthInsuranceNumber()).hasSize(80);
        assertThat(data.emergencyContactRelationship()).hasSize(80);
        assertThat(data.observations()).hasSize(2000);
    }

    @ParameterizedTest(name = "rejects {0} over its limit")
    @MethodSource("overLimitFields")
    void rejectsEveryTextFieldOverItsLimit(
            String fieldName,
            Function<String, MedicalProfileData> factory,
            int maxLength
    ) {
        assertThatThrownBy(() -> factory.apply("x".repeat(maxLength + 1)))
                .isInstanceOf(InvalidMedicalProfileException.class)
                .hasMessageContaining(fieldName)
                .hasMessageContaining(String.valueOf(maxLength));
    }

    @ParameterizedTest
    @MethodSource("invalidPhones")
    void rejectsInvalidInformedPhones(String phone) {
        assertThatThrownBy(() -> withPhone(phone))
                .isInstanceOf(InvalidMedicalProfileException.class)
                .hasMessageContaining("10 ou 11 dígitos");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "12345678901"})
    void acceptsTenAndElevenDigitPhones(String phone) {
        assertThat(withPhone(phone).emergencyContactPhone()).isEqualTo(phone);
    }

    private static Stream<Arguments> overLimitFields() {
        return Stream.of(
                Arguments.of("allergies", (Function<String, MedicalProfileData>) value ->
                        data(value, null, null, null, null, null, null, null, null, null), 2000),
                Arguments.of("chronicConditions", (Function<String, MedicalProfileData>) value ->
                        data(null, value, null, null, null, null, null, null, null, null), 2000),
                Arguments.of("continuousMedications", (Function<String, MedicalProfileData>) value ->
                        data(null, null, value, null, null, null, null, null, null, null), 2000),
                Arguments.of("healthInsuranceProvider", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, value, null, null, null, null, null, null), 120),
                Arguments.of("healthInsurancePlan", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, null, value, null, null, null, null, null), 120),
                Arguments.of("healthInsuranceNumber", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, null, null, value, null, null, null, null), 80),
                Arguments.of("emergencyContactName", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, null, null, null, value, null, null, null), 120),
                Arguments.of("emergencyContactRelationship", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, null, null, null, null, null, value, null), 80),
                Arguments.of("observations", (Function<String, MedicalProfileData>) value ->
                        data(null, null, null, null, null, null, null, null, null, value), 2000)
        );
    }

    private static Stream<String> invalidPhones() {
        return Stream.of(
                "123456789",
                "123456789012",
                "sem telefone"
        );
    }

    private static MedicalProfileData withPhone(String phone) {
        return data(null, null, null, null, null, null, null, phone, null, null);
    }

    private static MedicalProfileData data(
            String allergies,
            String chronicConditions,
            String continuousMedications,
            String provider,
            String plan,
            String insuranceNumber,
            String contactName,
            String phone,
            String relationship,
            String observations
    ) {
        return new MedicalProfileData(
                null,
                allergies,
                chronicConditions,
                continuousMedications,
                provider,
                plan,
                insuranceNumber,
                contactName,
                phone,
                relationship,
                observations
        );
    }
}
