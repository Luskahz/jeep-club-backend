package com.jeepclub.backend.identity.api.http.controller;

import com.jeepclub.backend.iam.identity.api.http.controller.UserRegistrationController;
import com.jeepclub.backend.iam.identity.api.http.exception.IdentityUserExceptionHandler;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import tools.jackson.databind.cfg.DateTimeFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

    private static final Instant NOW =
            Instant.parse("2026-09-01T12:00:00Z");

    @Mock
    private UserRegistration userRegistration;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        UserRegistrationController controller = new UserRegistrationController(
                userRegistration,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new IdentityUserExceptionHandler()
                )
                .setMessageConverters(
                        new JacksonJsonHttpMessageConverter(jsonMapper)
                )
                .setValidator(validator)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224725",
            "529.982.247-25"
    })
    void registersUsingBothSupportedCpfFormats(String cpf) throws Exception {
        when(userRegistration.registerAndAuthenticate(
                any(),
                eq("senha123")
        )).thenReturn(
                new UserAuthenticationTokens(
                        "refresh-reg",
                        "access-reg",
                        3600L
                )
        );

        String payload = """
                {
                  "name": "Teste",
                  "birthDate": "1990-01-01",
                  "email": "teste@email.com",
                  "cpf": "%s",
                  "rg": "12.345.67",
                  "password": "senha123",
                  "phoneNumber": "(11) 99999-9999"
                }
                """.formatted(cpf);

        mockMvc.perform(
                        post("/identity/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-reg"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-reg"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        ArgumentCaptor<UserRegistrationData> captor =
                ArgumentCaptor.forClass(UserRegistrationData.class);

        verify(userRegistration).registerAndAuthenticate(
                captor.capture(),
                eq("senha123")
        );

        UserRegistrationData registrationData = captor.getValue();

        assertThat(registrationData.birthDate())
                .isEqualTo(LocalDate.of(1990, 1, 1));

        assertThat(registrationData.cpf())
                .isEqualTo(cpf);

        assertThat(registrationData.now())
                .isEqualTo(NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "529 982 247 25",
            "529-982-247-25",
            "ABC52998224725"
    })
    void rejectsCpfFormatsOutsideTheHttpContract(String cpf) throws Exception {
        String payload = """
                {
                  "name": "Teste",
                  "cpf": "%s",
                  "password": "senha123"
                }
                """.formatted(cpf);

        mockMvc.perform(
                        post("/identity/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInvalidRegistrationData() throws Exception {
        String payload = """
                {
                  "name": "",
                  "cpf": "123",
                  "password": ""
                }
                """;

        mockMvc.perform(
                        post("/identity/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest());
    }
}