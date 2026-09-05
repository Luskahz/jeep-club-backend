package com.jeepclub.backend.authentication.api.contract;

import com.jeepclub.backend.iam.authentication.api.http.dto.recovery.PasswordRecoveryRequestResponseDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.session.LoginResponseDTO;
import com.jeepclub.backend.iam.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationDtoContractCharacterizationTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Test
    void passwordChangeLoginKeepsNamesDateFormatAndNullOmission() throws Exception {
        LoginResponseDTO response = LoginResponseDTO.from(
                new PasswordChangeRequiredLoginResult(
                        "challenge-token",
                        Instant.parse("2026-05-21T20:30:00Z")
                )
        );

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsBytes(response));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
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

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsBytes(response));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
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
