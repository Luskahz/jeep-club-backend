package com.jeepclub.backend.identity.api.contract;

import com.jeepclub.backend.iam.identity.api.http.dto.user.UserRegistrationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoContractCharacterizationTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Test
    void registrationUsesBirthDatePropertyWithoutLegacyAlias() throws Exception {
        String json = """
                {
                  "name": "Lucas Alves",
                  "birthDate": "2000-05-17",
                  "email": "lucas@example.com",
                  "cpf": "52998224725",
                  "rg": "1234567",
                  "password": "Senha@123",
                  "phoneNumber": "5512999999999"
                }
                """;

        UserRegistrationRequestDTO request = jsonMapper.readValue(json, UserRegistrationRequestDTO.class);

        assertThat(request.birthDate()).isEqualTo(LocalDate.of(2000, 5, 17));
        JsonNode serialized = jsonMapper.readTree(jsonMapper.writeValueAsBytes(request));
        assertThat(serialized.has("birthDate")).isTrue();
        assertThat(serialized.has("birthData")).isFalse();
    }
}
