package com.jeepclub.backend.memberships.api.security;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.export.ApplicantBlockExportService;
import com.jeepclub.backend.memberships.core.application.service.export.MembershipApplicationExportService;
import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.MembershipApplicationService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.shared.export.ExportFile;
import com.jeepclub.backend.shared.export.ExportFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MembershipAdminSecurityIntegrationTest {

    @org.junit.jupiter.params.ParameterizedTest(name = "{0} {1} rejects unrelated authority")
    @org.junit.jupiter.params.provider.CsvSource({
            "POST, /admin/membership-applications/1/approve/temporary-password",
            "POST, /admin/membership-applications/1/approve/access-link",
            "POST, /admin/membership-applications/1/activation-link/resend",
            "POST, /admin/membership-applications/1/reject",
            "POST, /admin/membership-applications/1/reject-and-block",
            "POST, /admin/membership-applications/blocks/52998224725/unblock",
            "GET, /admin/membership-applications/export",
            "GET, /admin/membership-applications/blocks/export"
    })
    void mutationAndExportEndpointsRejectReadOnlyAuthority(String method, String path) throws Exception {
        authenticate("read-only-audit", 107L, List.of("MEMBERSHIP_MEMBERSHIP_REQUEST_READ"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                        org.springframework.http.HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer read-only-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Audit rejection\"}"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        org.mockito.Mockito.verifyNoInteractions(adminMembershipApplicationService,
                adminMembershipApplicantBlockService, membershipApplicationExportService,
                applicantBlockExportService);
    }

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMembershipApplicationService adminMembershipApplicationService;

    @MockitoBean
    private AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;

    @MockitoBean
    private MembershipApplicationService membershipApplicationService;

    @MockitoBean
    private MemberActivationTokenService memberActivationTokenService;

    @MockitoBean
    private ApplicantBlockExportService applicantBlockExportService;

    @MockitoBean
    private MembershipApplicationExportService membershipApplicationExportService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    @DisplayName("Public routes do not require authentication header")
    void publicRoutesAllowAnonymousAccess() throws Exception {
        MembershipApplication app = sampleApplication(1L, MembershipApplicationStatus.PENDING);
        when(membershipApplicationService.ensure(any(), any(), any(), any(), any()))
                .thenReturn(EnsureMembershipRequestResult.created(app));

        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lucas Silva",
                                  "cpf": "52998224725",
                                  "phoneNumber": "11988887777"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", "raw-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Unauthenticated request to admin membership applications returns 401")
    void unauthenticatedAdminAccessReturns401() throws Exception {
        mockMvc.perform(get("/admin/membership-applications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/membership-applications/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/1/approve/temporary-password"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/1/approve/access-link"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/1/activation-link/resend"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/1/reject"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/1/reject-and-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fraude\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/membership-applications/blocks/52998224725/unblock"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/membership-applications/export"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/membership-applications/blocks/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user without MEMBERSHIP_MEMBERSHIP_REQUEST_READ gets 403 on list and getById")
    void missingReadAuthorityReturns403() throws Exception {
        authenticate("token-no-perm", 100L, List.of("SOME_OTHER_PERMISSION"));

        mockMvc.perform(get("/admin/membership-applications")
                        .header("Authorization", "Bearer token-no-perm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/admin/membership-applications/1")
                        .header("Authorization", "Bearer token-no-perm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_MEMBERSHIP_REQUEST_READ can list and getById")
    void readAuthorityAllowsAccess() throws Exception {
        authenticate("token-read", 101L, List.of("MEMBERSHIP_MEMBERSHIP_REQUEST_READ"));
        when(adminMembershipApplicationService.listAll(any()))
                .thenReturn(new PageImpl<>(List.of(sampleApplication(1L, MembershipApplicationStatus.PENDING))));
        when(adminMembershipApplicationService.findById(1L))
                .thenReturn(sampleApplication(1L, MembershipApplicationStatus.PENDING));

        mockMvc.perform(get("/admin/membership-applications")
                        .header("Authorization", "Bearer token-read"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/membership-applications/1")
                        .header("Authorization", "Bearer token-read"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE can approve and resend")
    void approveAuthorityAllowsApprovalEndpoints() throws Exception {
        authenticate("token-approve", 102L, List.of("MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE"));
        when(adminMembershipApplicationService.approveWithTemporaryPassword(eq(1L), eq(102L)))
                .thenReturn(new PendingFirstAccessUser(200L, "Temp#Pass"));
        when(adminMembershipApplicationService.approveWithAccessLink(eq(1L), eq(102L)))
                .thenReturn(new com.jeepclub.backend.memberships.core.port.AccessLinkApproval(200L, "https://app.example/activate?token=test-bearer"));

        mockMvc.perform(post("/admin/membership-applications/1/approve/temporary-password")
                        .header("Authorization", "Bearer token-approve"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/membership-applications/1/approve/access-link")
                        .header("Authorization", "Bearer token-approve"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/membership-applications/1/activation-link/resend")
                        .header("Authorization", "Bearer token-approve"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT can reject application")
    void rejectAuthorityAllowsRejectEndpoint() throws Exception {
        authenticate("token-reject", 103L, List.of("MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT"));

        mockMvc.perform(post("/admin/membership-applications/1/reject")
                        .header("Authorization", "Bearer token-reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inconsistência cadastral\"}"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicationService).reject(1L, 103L, "Inconsistência cadastral");
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK can reject and block")
    void blockAuthorityAllowsRejectAndBlockEndpoint() throws Exception {
        authenticate("token-block", 104L, List.of("MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK"));

        mockMvc.perform(post("/admin/membership-applications/1/reject-and-block")
                        .header("Authorization", "Bearer token-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fraude confirmada\"}"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicationService).rejectAndBlock(1L, 104L, "Fraude confirmada");
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK can unblock CPF")
    void unblockAuthorityAllowsUnblockEndpoint() throws Exception {
        authenticate("token-unblock", 105L, List.of("MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK"));

        mockMvc.perform(post("/admin/membership-applications/blocks/52998224725/unblock")
                        .header("Authorization", "Bearer token-unblock"))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicantBlockService).unblock("52998224725", 105L);
    }

    @Test
    @DisplayName("Authenticated user with MEMBERSHIP_EXPORT can access CSV/PDF export endpoints")
    void exportAuthorityAllowsExportEndpoints() throws Exception {
        authenticate("token-export", 106L, List.of("MEMBERSHIP_EXPORT"));
        when(membershipApplicationExportService.export(eq(ExportFormat.CSV), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ExportFile("relatorio.csv", "text/csv", "id,name\n1,Lucas".getBytes()));
        when(applicantBlockExportService.export(eq(ExportFormat.CSV), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ExportFile("bloqueios.csv", "text/csv", "cpf,reason\n52998224725,Fraude".getBytes()));

        mockMvc.perform(get("/admin/membership-applications/export")
                        .header("Authorization", "Bearer token-export"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/membership-applications/blocks/export")
                        .header("Authorization", "Bearer token-export"))
                .andExpect(status().isOk());
    }

    private void authenticate(String token, Long userId, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token)).thenReturn(
                new JwtAuthenticatedUser(userId, 1L, "Admin User", NOW.plusSeconds(3600))
        );
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(userId)).thenReturn(authorities);
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
