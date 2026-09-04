package com.jeepclub.backend.identity.api.http.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.identity.api.http.exception.IdentityUserExceptionHandler;
import com.jeepclub.backend.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserRegistrationControllerTest {
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Mock
    private UserRegistration userRegistration;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        UserRegistrationController controller = new UserRegistrationController(
                userRegistration,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new IdentityUserExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void registersUsingBirthDateAndReturnsTokens() throws Exception {
        when(userRegistration.registerAndAuthenticate(any(), eq("senha123")))
                .thenReturn(new UserAuthenticationTokens("refresh-reg", "access-reg", 3600L));

        String payload = """
                {
                  "name": "Teste",
                  "birthDate": "1990-01-01",
                  "email": "teste@email.com",
                  "cpf": "529.982.247-25",
                  "rg": "12.345.67",
                  "password": "senha123",
                  "phoneNumber": "(11) 99999-9999"
                }
                """;

        mockMvc.perform(post("/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-reg"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-reg"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        var captor = org.mockito.ArgumentCaptor.forClass(UserRegistrationData.class);
        verify(userRegistration).registerAndAuthenticate(captor.capture(), eq("senha123"));
        assertThat(captor.getValue().birthDate()).isEqualTo(java.time.LocalDate.of(1990, 1, 1));
        assertThat(captor.getValue().cpf()).isEqualTo("529.982.247-25");
        assertThat(captor.getValue().now()).isEqualTo(NOW);
    }

    @Test
    void rejectsInvalidRegistrationData() throws Exception {
        mockMvc.perform(post("/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"cpf\":\"123\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
