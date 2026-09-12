package com.jeepclub.backend.health.api.http.exception;

import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileConflictException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceUnavailableException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalProfileExceptionHandlerTest {

    private final MedicalProfileExceptionHandler handler =
            new MedicalProfileExceptionHandler();

    @Test
    void mapsMissingOwnerToStandardNotFoundProblem() {
        var response = handler.handleMedicalProfileOwnerNotFound(
                new MedicalProfileOwnerNotFoundException(
                        MedicalProfileOwnerType.USER,
                        404L
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("MEDICAL_PROFILE_OWNER_NOT_FOUND");
    }

    @Test
    void mapsInactiveOwnerToStandardConflictProblem() {
        var response = handler.handleMedicalProfileOwnerInactive(
                new MedicalProfileOwnerInactiveException(
                        MedicalProfileOwnerType.DEPENDENT,
                        11L
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("MEDICAL_PROFILE_OWNER_INACTIVE");
    }

    @Test
    void domainAndApplicationValidationUseTheSameHttpContract() {
        var domainResponse = handler.handleInvalidMedicalProfileData(
                new InvalidMedicalProfileException("dado inválido")
        );
        var applicationResponse = handler.handleInvalidMedicalProfileData(
                new InvalidMedicalProfileDataException("id inválido")
        );

        assertThat(domainResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(applicationResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(domainResponse.getBody()).isNotNull();
        assertThat(applicationResponse.getBody()).isNotNull();
        assertThat(domainResponse.getBody().getCode())
                .isEqualTo("MEDICAL_PROFILE_INVALID_DATA");
        assertThat(applicationResponse.getBody().getCode())
                .isEqualTo("MEDICAL_PROFILE_INVALID_DATA");
    }

    @Test
    void persistenceErrorsNeverExposeInfrastructureDetails() {
        String infrastructureDetail =
                "SQLSTATE 23000 constraint uk_medical_profile_owner at com.mysql.Driver";

        var conflict = handler.handleMedicalProfileConflict(
                MedicalProfileConflictException.ownerAlreadyHasProfile(
                        MedicalProfileOwnerType.USER,
                        7L,
                        new RuntimeException(infrastructureDetail)
                )
        );
        var unavailable = handler.handlePersistenceUnavailable(
                new MedicalProfilePersistenceUnavailableException(
                        new RuntimeException(infrastructureDetail)
                )
        );
        var failure = handler.handlePersistenceFailure(
                new MedicalProfilePersistenceException(
                        new RuntimeException(infrastructureDetail)
                )
        );

        assertSafeError(conflict, "MEDICAL_PROFILE_CONFLICT", HttpStatus.CONFLICT,
                infrastructureDetail);
        assertSafeError(unavailable, "MEDICAL_PROFILE_PERSISTENCE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE, infrastructureDetail);
        assertSafeError(failure, "MEDICAL_PROFILE_PERSISTENCE_FAILURE",
                HttpStatus.INTERNAL_SERVER_ERROR, infrastructureDetail);
    }

    private void assertSafeError(
            org.springframework.http.ResponseEntity<com.jeepclub.backend.platform.web.exception.ApiErrorResponse> response,
            String code,
            HttpStatus status,
            String infrastructureDetail
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(code);
        assertThat(response.getBody().getDetail())
                .doesNotContain(infrastructureDetail)
                .doesNotContain("SQLSTATE")
                .doesNotContain("constraint")
                .doesNotContain("com.mysql");
    }
}
