package com.jeepclub.backend.health.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthOpenApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void openApiDescribesHealthContractsPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/MedicalProfileResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['put']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/dependents/{dependentId}']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/dependents/{dependentId}']['delete']['responses']['204']")
                        .exists())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['x-required-permissions'][0]")
                        .value("HEALTH_MEDICAL_PROFILE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'page')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'size')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'sort')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageMedicalProfileSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageMedicalProfileSummaryResponse']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageMedicalProfileSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MedicalProfileSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/users/{userId}']['put']['x-required-permissions'][0]")
                        .value("HEALTH_MEDICAL_PROFILE_UPDATE"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/users/{userId}']['put']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/MedicalProfileMutationResponse"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/{profileId}']['delete']['x-required-permissions'][0]")
                        .value("HEALTH_MEDICAL_PROFILE_DELETE"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/{profileId}']['delete']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/{profileId}']['delete']['responses']['409']['content']['application/json']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['MedicalProfileRequest']['properties']['emergencyContactPhone']['description']")
                        .value(org.hamcrest.Matchers.containsString("10 ou 11 dígitos")));
    }
}
