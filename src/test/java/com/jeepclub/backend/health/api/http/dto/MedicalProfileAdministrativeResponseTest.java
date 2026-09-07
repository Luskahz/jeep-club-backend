package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalProfileAdministrativeResponseTest {

    @Test
    void mutationResponseDoesNotEchoClinicalContent() {
        assertThat(componentNames(MedicalProfileMutationResponse.class))
                .containsExactlyInAnyOrder("id", "ownerType", "ownerId", "updatedAt");
    }

    @Test
    void listSummaryDoesNotExposeClinicalContent() {
        assertThat(componentNames(MedicalProfileSummaryResponse.class))
                .containsExactlyInAnyOrder("id", "ownerType", "ownerId", "updatedAt");
    }

    @Test
    void sensitiveRequestCommandAndResponseHaveRedactedStringRepresentations() {
        String secret = "CONTEUDO-MEDICO-SECRETO";
        MedicalProfileRequest request = new MedicalProfileRequest(
                BloodType.O_POSITIVE,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret
        );
        UpsertMedicalProfileCommand command = request.toApplicationData();
        MedicalProfileResponse response = new MedicalProfileResponse(
                1L,
                MedicalProfileOwnerType.USER,
                7L,
                BloodType.O_POSITIVE,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                secret,
                Instant.EPOCH,
                Instant.EPOCH
        );

        assertThat(request.toString()).doesNotContain(secret);
        assertThat(command.toString()).doesNotContain(secret);
        assertThat(response.toString()).doesNotContain(secret);
    }

    @Test
    void requestDtoDoesNotDuplicateDomainValidationRules() {
        boolean hasBeanValidationRule = Stream.of(
                        MedicalProfileRequest.class.getRecordComponents()
                )
                .flatMap(component -> Arrays.stream(component.getAnnotations()))
                .map(annotation -> annotation.annotationType().getPackageName())
                .anyMatch(packageName -> packageName.startsWith("jakarta.validation"));

        assertThat(hasBeanValidationRule).isFalse();
    }

    private Set<String> componentNames(Class<?> recordType) {
        return Stream.of(recordType.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
    }
}
