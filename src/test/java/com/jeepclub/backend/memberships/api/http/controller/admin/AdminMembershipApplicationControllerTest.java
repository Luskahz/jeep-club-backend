package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.api.http.exception.MembershipExceptionHandler;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminMembershipApplicationControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Long ADMIN_USER_ID = 10L;

    @Mock
    private AdminMembershipApplicationService adminMembershipApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        UserPrincipal principal = new UserPrincipal(ADMIN_USER_ID, 1L, "Admin", Instant.now());

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminMembershipApplicationController(adminMembershipApplicationService)
                )
                .setControllerAdvice(new MembershipExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(UserPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return principal;
                            }
                        }
                )
                .build();
    }

    @Test
    @DisplayName("GET /admin/membership-applications without status filter returns page of applications")
    void listAllReturnsPageOfApplications() throws Exception {
        MembershipApplication app = sampleApplication(1L, MembershipApplicationStatus.PENDING);
        when(adminMembershipApplicationService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));

        mockMvc.perform(get("/admin/membership-applications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Lucas Silva"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(adminMembershipApplicationService).listAll(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/membership-applications with status filter returns filtered applications")
    void listByStatusReturnsFilteredApplications() throws Exception {
        MembershipApplication app = sampleApplication(1L, MembershipApplicationStatus.PENDING);
        when(adminMembershipApplicationService.listByStatus(eq(MembershipApplicationStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));

        mockMvc.perform(get("/admin/membership-applications")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(adminMembershipApplicationService).listByStatus(eq(MembershipApplicationStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/membership-applications/{id} returns application DTO when found")
    void getByIdReturnsApplicationWhenExists() throws Exception {
        MembershipApplication app = sampleApplication(123L, MembershipApplicationStatus.PENDING);
        when(adminMembershipApplicationService.findById(123L)).thenReturn(app);

        mockMvc.perform(get("/admin/membership-applications/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123L))
                .andExpect(jsonPath("$.name").value("Lucas Silva"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /admin/membership-applications/{id} returns RFC 9457 problem when application does not exist")
    void getByIdReturnsRfc9457ProblemWhenApplicationDoesNotExist() throws Exception {
        when(adminMembershipApplicationService.findById(999L))
                .thenThrow(new MembershipApplicationNotFoundException(999L));

        mockMvc.perform(get("/admin/membership-applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_APPLICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/approve/temporary-password returns temporary password")
    void approveWithTemporaryPasswordReturnsSuccess() throws Exception {
        when(adminMembershipApplicationService.approveWithTemporaryPassword(1L, ADMIN_USER_ID))
                .thenReturn(new PendingFirstAccessUser(456L, "Temp#Pass2026"));

        mockMvc.perform(post("/admin/membership-applications/1/approve/temporary-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(456L))
                .andExpect(jsonPath("$.temporaryPassword").value("Temp#Pass2026"));

        verify(adminMembershipApplicationService).approveWithTemporaryPassword(1L, ADMIN_USER_ID);
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/approve/access-link returns access link result")
    void approveWithAccessLinkReturnsSuccess() throws Exception {
        when(adminMembershipApplicationService.approveWithAccessLink(1L, ADMIN_USER_ID))
                .thenReturn(new PendingFirstAccessIdentity(456L));

        mockMvc.perform(post("/admin/membership-applications/1/approve/access-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(456L));

        verify(adminMembershipApplicationService).approveWithAccessLink(1L, ADMIN_USER_ID);
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/activation-link/resend returns 204 No Content")
    void resendActivationLinkReturnsNoContent() throws Exception {
        doNothing().when(adminMembershipApplicationService).resendActivationLink(1L);

        mockMvc.perform(post("/admin/membership-applications/1/activation-link/resend"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicationService).resendActivationLink(1L);
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/reject returns 204 No Content")
    void rejectReturnsNoContent() throws Exception {
        doNothing().when(adminMembershipApplicationService).reject(1L, ADMIN_USER_ID, "Inconformidade");

        mockMvc.perform(post("/admin/membership-applications/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inconformidade\"}"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicationService).reject(1L, ADMIN_USER_ID, "Inconformidade");
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/reject-and-block returns 204 No Content")
    void rejectAndBlockReturnsNoContent() throws Exception {
        doNothing().when(adminMembershipApplicationService).rejectAndBlock(1L, ADMIN_USER_ID, "Fraude cadastral comprovada");

        mockMvc.perform(post("/admin/membership-applications/1/reject-and-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fraude cadastral comprovada\"}"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicationService).rejectAndBlock(1L, ADMIN_USER_ID, "Fraude cadastral comprovada");
    }

    @Test
    @DisplayName("POST /admin/membership-applications/{id}/reject-and-block returns 400 Bad Request when reason is blank")
    void rejectAndBlockReturns400WhenReasonBlank() throws Exception {
        mockMvc.perform(post("/admin/membership-applications/1/reject-and-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST operations on already processed application return 409 Conflict RFC 9457")
    void operationsOnProcessedApplicationReturn409() throws Exception {
        when(adminMembershipApplicationService.approveWithTemporaryPassword(1L, ADMIN_USER_ID))
                .thenThrow(new MembershipApplicationAlreadyProcessedException(1L, "APPROVED"));

        mockMvc.perform(post("/admin/membership-applications/1/approve/temporary-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_APPLICATION_ALREADY_PROCESSED"))
                .andExpect(jsonPath("$.status").value(409));
    }

    private MembershipApplication sampleApplication(Long id, MembershipApplicationStatus status) {
        return MembershipApplication.reconstitute(
                id,
                "Lucas Silva",
                "52998224725",
                "lucas@example.com",
                "11988887777",
                "Mensagem",
                status,
                null,
                null,
                null,
                NOW.minusSeconds(3600),
                null,
                null,
                NOW.minusSeconds(3600),
                0L
        );
    }
}
