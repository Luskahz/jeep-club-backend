package com.jeepclub.backend.authentication.api.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.iam.authentication.api.http.dto.recovery.PasswordRecoveryRequestResponseDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.session.LoginResponseDTO;
import com.jeepclub.backend.iam.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationDtoContractCharacterizationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void passwordChangeLoginKeepsNamesDateFormatAndNullOmission() throws Exception {
        LoginResponseDTO response = LoginResponseDTO.from(
                new PasswordChangeRequiredLoginResult(
                        "challenge-token",
                        Instant.parse("2026-05-21T20:30:00Z")
                )
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "status",
                "passwordChangeToken",
                "passwordChangeTokenExpiresAt"
        );
        assertThat(json.get("status").asText()).isEqualTo("PASSWORD_CHANGE_REQUIRED");
        assertThat(json.get("passwordChangeTokenExpiresAt").asText())
                .isEqualTo("2026-05-21T20:30:00Z");
    }

    @Test
    void recoveryResponseKeepsEnumValuesDatesAndNullableFields() throws Exception {
        PasswordRecoveryRequestResponseDTO response =
                PasswordRecoveryRequestResponseDTO.from(
                        new PublicPasswordRecoveryResult(
                                PasswordRecoveryRequestStatus.OPEN,
                                PasswordRecoveryRequestMethod.UNDEFINED,
                                Instant.parse("2026-05-21T18:00:00Z"),
                                Instant.parse("2026-05-28T18:00:00Z"),
                                null,
                                null
                        )
                );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "status",
                "method",
                "createdAt",
                "expiresAt",
                "resolvedAt",
                "cancelledAt"
        );
        assertThat(json.get("status").asText()).isEqualTo("OPEN");
        assertThat(json.get("method").asText()).isEqualTo("UNDEFINED");
        assertThat(json.get("createdAt").asText()).isEqualTo("2026-05-21T18:00:00Z");
        assertThat(json.get("resolvedAt").isNull()).isTrue();
        assertThat(json.get("cancelledAt").isNull()).isTrue();
    }
}
