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
    void administrativePresentationIsAdditiveAndRestrictedToExistingReadRoutes() throws Exception {
        var document = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        for (String schema : java.util.List.of("MedicalProfileResponse", "MedicalProfileSummaryResponse")) {
            document.andExpect(jsonPath("$['components']['schemas']['" + schema + "']['properties']['ownerName']['type']").value("string"))
                    .andExpect(jsonPath("$['components']['schemas']['" + schema + "']['properties']['responsibleName']").exists())
                    .andExpect(jsonPath("$['components']['schemas']['" + schema + "']['properties']['cpf']").doesNotExist())
                    .andExpect(jsonPath("$['components']['schemas']['" + schema + "']['properties']['email']").doesNotExist());
        }
        for (String route : java.util.List.of("/admin/medical-profiles", "/admin/medical-profiles/{profileId}",
                "/admin/medical-profiles/users/{userId}", "/admin/medical-profiles/dependents/{dependentId}")) {
            document.andExpect(jsonPath("$['paths']['" + route + "']['get']['x-required-permissions'][0]").value("HEALTH_MEDICAL_PROFILE_READ"))
                    .andExpect(jsonPath("$['paths']['" + route + "']['get']['responses']['200']['headers']['Cache-Control']['schema']['example']").value("no-store"));
        }
        document.andExpect(jsonPath("$['paths']['/admin/medical-profiles/{profileId}/owner']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/identity/admin/users/options']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['PersonalMedicalProfileResponse']['properties']['ownerName']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['PersonalMedicalProfileResponse']['properties']['responsibleName']").doesNotExist());
    }

    @Test
    void remainingOperationalContractsMatchActualMethodsPermissionsAndPrivacySchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/{profileId}']['get']['x-required-permissions'][0]").value("HEALTH_MEDICAL_PROFILE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/users/{userId}']['get']['x-required-permissions'][0]").value("HEALTH_MEDICAL_PROFILE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/dependents/{dependentId}']['get']['x-required-permissions'][0]").value("HEALTH_MEDICAL_PROFILE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles/dependents/{dependentId}']['put']['x-required-permissions'][0]").value("HEALTH_MEDICAL_PROFILE_UPDATE"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['post']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['patch']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['MedicalProfileMutationResponse']['properties']['allergies']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['MedicalProfileSummaryResponse']['properties']['allergies']").doesNotExist());
    }

    @Test
    void openApiDescribesHealthContractsPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PersonalMedicalProfileResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/me']['put']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/dependents/{dependentId}']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/medical-profiles/dependents/{dependentId}']['delete']['responses']['204']")
                        .exists())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['x-required-permissions'][0]")
                        .value("HEALTH_MEDICAL_PROFILE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'page')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'size')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['parameters'][?(@.name == 'sort')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$['paths']['/admin/medical-profiles']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMedicalProfileSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMedicalProfileSummaryResponse']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMedicalProfileSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MedicalProfileSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMedicalProfileSummaryResponse']['properties']['numberOfElements']['format']")
                        .value("int32"))
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
