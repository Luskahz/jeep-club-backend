package com.jeepclub.backend.authentication.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.authentication.api.http.controller.UserController;
import com.jeepclub.backend.authentication.api.http.exception.UserExceptionHandler;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.service.user.UserService;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        UserController controller = new UserController(userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new UserExceptionHandler()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Sucesso: registro com birthData no JSON mantÃ©m compatibilidade e retorna 201")
    void shouldReturnTokensOnSuccessfulRegistration() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-reg",
                "access-reg",
                3600L
        );

        when(userService.registerAndAuthenticate(
                eq("Teste"),
                eq(LocalDate.of(1990, 1, 1)),
                eq("teste@email.com"),
                eq("52998224725"),
                eq("1234567"),
                eq("senha123"),
                eq("11999999999")
        )).thenReturn(tokens);

        String payload = """
                {
                  "name": "Teste",
                  "birthData": "1990-01-01",
                  "email": "teste@email.com",
                  "cpf": "52998224725",
                  "rg": "1234567",
                  "password": "senha123",
                  "phoneNumber": "11999999999"
                }
                """;

        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-reg"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-reg"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("Falha: dados invÃ¡lidos retornam 400 Bad Request")
    void shouldReturnBadRequestOnInvalidData() throws Exception {
        String invalidPayload = """
                {
                  "name": "",
                  "cpf": "123",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
