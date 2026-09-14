package com.jeepclub.backend.dependents.api.contract;

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
class DependentsOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDescribesDependentsContractsPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/dependents']['post']['responses']['201']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/DependentResponseDTO"))
                .andExpect(jsonPath("$['paths']['/dependents']['post']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/dependents']['post']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/dependents']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/dependents']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/DependentResponseDTO"))
                .andExpect(jsonPath("$['paths']['/dependents/{id}']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/dependents/{id}']['put']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/dependents/{id}']['delete']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents']['get']['x-required-permissions'][0]")
                        .value("DEPENDENTS_DEPENDENT_READ"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/DependentResponseDTO"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents/{id}']['get']['responses']['404']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"));
    }
}
