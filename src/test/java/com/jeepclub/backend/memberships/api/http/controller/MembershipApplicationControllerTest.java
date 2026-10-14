package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.exception.MembershipExceptionHandler;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipCpfAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyInUseException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.MembershipApplicationService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MembershipApplicationControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final String VALID_CPF = "52998224725";

    @Mock
    private MembershipApplicationService membershipApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MembershipApplicationController(membershipApplicationService)
                )
                .setControllerAdvice(new MembershipExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    @DisplayName("Should return 201 Created when new application is successfully created")
    void shouldReturn201WhenCreated() throws Exception {
        MembershipApplication application = MembershipApplication.create(
                "Lucas Silva", VALID_CPF, "lucas@example.com", "11988887777", "Msg", NOW
        );
        when(membershipApplicationService.ensure("Lucas Silva", VALID_CPF, "lucas@example.com", "11988887777", "Msg"))
                .thenReturn(EnsureMembershipRequestResult.created(application));

        String requestBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "email": "lucas@example.com",
                  "phoneNumber": "11988887777",
                  "message": "Msg"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    @DisplayName("Should return 200 OK when application already exists in PENDING status (idempotent submission)")
    void shouldReturn200WhenExistingPending() throws Exception {
        MembershipApplication application = MembershipApplication.create(
                "Lucas Silva", VALID_CPF, "lucas@example.com", "11988887777", null, NOW
        );
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenReturn(EnsureMembershipRequestResult.existing(application));

        String requestBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "email": "lucas@example.com",
                  "phoneNumber": "11988887777"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    @DisplayName("Should return 400 Bad Request RFC 9457 when request body fails validation")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        String invalidBody = """
                {
                  "name": "   ",
                  "cpf": "123",
                  "email": "not-an-email",
                  "phoneNumber": " "
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden RFC 9457 when applicant is blocked")
    void shouldReturn403WhenApplicantBlocked() throws Exception {
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenThrow(new MembershipApplicantBlockedException());

        String validBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "phoneNumber": "11988887777"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_APPLICATION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Should return 409 Conflict RFC 9457 when CPF is already registered")
    void shouldReturn409WhenCpfAlreadyRegistered() throws Exception {
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenThrow(new MembershipCpfAlreadyRegisteredException(VALID_CPF));

        String validBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "phoneNumber": "11988887777"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_CPF_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 409 Conflict RFC 9457 when email is already in use by another pending application")
    void shouldReturn409WhenEmailAlreadyInUse() throws Exception {
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenThrow(new MembershipEmailAlreadyInUseException("lucas@example.com"));

        String validBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "email": "lucas@example.com",
                  "phoneNumber": "11988887777"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_EMAIL_ALREADY_IN_USE"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 409 Conflict RFC 9457 when email is already registered to an existing user")
    void shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenThrow(new MembershipEmailAlreadyRegisteredException("lucas@example.com"));

        String validBody = """
                {
                  "name": "Lucas Silva",
                  "cpf": "52998224725",
                  "email": "lucas@example.com",
                  "phoneNumber": "11988887777"
                }
                """;

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
