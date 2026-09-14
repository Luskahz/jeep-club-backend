package com.jeepclub.backend.memberships.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MembershipOpenApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void openApiDescribesMembershipContractsPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/membership-applications']['post']['responses']['201']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/MembershipApplicationResponseDTO"))
                .andExpect(jsonPath("$['paths']['/membership-applications']['post']['security']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/membership-applications']['post']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/membership-applications']['post']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/membership-applications/activate']['get']['responses']['410']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/membership-applications/activate']['get']['security']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['x-required-permissions'][0]")
                        .value("MEMBERSHIP_MEMBERSHIP_REQUEST_READ"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['parameters'][?(@.name == 'page')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['parameters'][?(@.name == 'size')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['parameters'][?(@.name == 'sort')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['parameters'][?(@.name == 'status')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/membership-applications']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageMembershipApplicationResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageMembershipApplicationResponseDTO']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageMembershipApplicationResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MembershipApplicationResponseDTO"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/{id}/approve/temporary-password']['post']['x-required-permissions'][0]")
                        .value("MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/{id}/approve/temporary-password']['post']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/TemporaryPasswordApprovalResponseDTO"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/{id}/approve/access-link']['post']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/AccessLinkApprovalResponseDTO"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/{id}/reject']['post']['x-required-permissions'][0]")
                        .value("MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/{id}/reject-and-block']['post']['x-required-permissions'][0]")
                        .value("MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/blocks/{cpf}/unblock']['post']['x-required-permissions'][0]")
                        .value("MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK"))
                .andExpect(jsonPath("$['paths']['/admin/membership-applications/blocks/{cpf}/unblock']['post']['responses']['404']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"));
    }

    @Test
    void publicMembershipErrorsUseRfc9457() throws Exception {
        mockMvc.perform(post("/membership-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/membership-applications/activate").param("token", "missing-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_NOT_FOUND"));
    }
}
