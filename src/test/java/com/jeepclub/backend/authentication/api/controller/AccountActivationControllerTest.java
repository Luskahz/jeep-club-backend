package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.api.http.controller.AccountActivationController;
import com.jeepclub.backend.iam.authentication.api.http.dto.activation.ActivationInvitationTokenDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.activation.CompleteAccountActivationDTO;
import com.jeepclub.backend.iam.authentication.api.http.exception.AuthenticationAccountExceptionHandler;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountActivationControllerTest {

    private static final String VALID_TOKEN = "valid.activation.invitation.token";

    @Mock
    private AccountActivationInvitationService invitationService;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AccountActivationController controller = new AccountActivationController(invitationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new AuthenticationAccountExceptionHandler()
                )
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sucesso: validação de convite ativo retorna 204 No Content com Cache-Control: no-store")
    void shouldValidateInvitationSuccessfully() throws Exception {
        doNothing().when(invitationService).validate(VALID_TOKEN);

        ActivationInvitationTokenDTO request = new ActivationInvitationTokenDTO(VALID_TOKEN);

        mockMvc.perform(post("/authentication/activation-invitations/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(invitationService).validate(VALID_TOKEN);
    }

    @Test
    @DisplayName("Falha: validação de convite expirado ou inválido retorna 410 Gone via RFC 9457")
    void shouldReturnGoneWhenInvitationIsExpiredOrInvalid() throws Exception {
        doThrow(new ActivationInvitationException(false))
                .when(invitationService).validate(VALID_TOKEN);

        ActivationInvitationTokenDTO request = new ActivationInvitationTokenDTO(VALID_TOKEN);

        mockMvc.perform(post("/authentication/activation-invitations/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("ACTIVATION_INVITATION_GONE"))
                .andExpect(jsonPath("$.detail").value("Invitation is invalid or expired."));
    }

    @Test
    @DisplayName("Sucesso: conclusão de ativação define senha permanente e retorna 204 No Content")
    void shouldCompleteActivationSuccessfully() throws Exception {
        doNothing().when(invitationService).complete(VALID_TOKEN, "NovaSenha@123");

        CompleteAccountActivationDTO request = new CompleteAccountActivationDTO(VALID_TOKEN, "NovaSenha@123");

        mockMvc.perform(post("/authentication/account-activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(invitationService).complete(VALID_TOKEN, "NovaSenha@123");
    }

    @Test
    @DisplayName("Falha: conclusão de ativação com conflito de ownership retorna 409 Conflict")
    void shouldReturnConflictWhenActivationHasConflict() throws Exception {
        doThrow(new ActivationInvitationException(true))
                .when(invitationService).complete(anyString(), anyString());

        CompleteAccountActivationDTO request = new CompleteAccountActivationDTO(VALID_TOKEN, "NovaSenha@123");

        mockMvc.perform(post("/authentication/account-activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ACTIVATION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Account is not eligible for activation."));
    }

    @Test
    @DisplayName("Falha: conclusão com senha menor que 8 caracteres retorna 400 Bad Request")
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        CompleteAccountActivationDTO request = new CompleteAccountActivationDTO(VALID_TOKEN, "123");

        mockMvc.perform(post("/authentication/account-activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sucesso: administrador reemite convite de ativação e retorna 202 Accepted")
    void shouldReissueInvitationSuccessfully() throws Exception {
        UserPrincipal principal = new UserPrincipal(99L, 1L, "Admin User", Instant.now().plusSeconds(3600));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("IDENTITY_USER_CREATE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(invitationService.deliveryStatus(10L)).thenReturn("PENDING");

        mockMvc.perform(post("/authentication/admin/users/{userId}/activation-invitations", 10L)
                        .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.invitationStatus").value("PENDING"));

        verify(invitationService).issueForExistingUser(10L, 99L);
    }

    @Test
    @DisplayName("Sucesso: consulta status de entrega do convite retorna 200 OK")
    void shouldReturnDeliveryStatus() throws Exception {
        when(invitationService.deliveryStatus(10L)).thenReturn("SENT");

        mockMvc.perform(get("/authentication/admin/users/{userId}/activation-invitations", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.invitationStatus").value("SENT"));

        verify(invitationService).deliveryStatus(10L);
    }
}
