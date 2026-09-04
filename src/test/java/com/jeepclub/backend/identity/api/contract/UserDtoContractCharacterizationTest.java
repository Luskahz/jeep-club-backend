package com.jeepclub.backend.identity.api.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UserRegistrationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoContractCharacterizationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

        UserRegistrationRequestDTO request = objectMapper.readValue(json, UserRegistrationRequestDTO.class);

        assertThat(request.birthDate()).isEqualTo(LocalDate.of(2000, 5, 17));
        JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsBytes(request));
        assertThat(serialized.has("birthDate")).isTrue();
        assertThat(serialized.has("birthData")).isFalse();
    }
}
