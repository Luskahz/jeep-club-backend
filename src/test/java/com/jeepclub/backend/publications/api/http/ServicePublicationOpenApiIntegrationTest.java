package com.jeepclub.backend.publications.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServicePublicationOpenApiIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test void bothServiceWorkflowsExposeDistinctPermissionsAndRealContracts() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/service-publication-requests']['post']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_REQUEST_CREATE"))
                .andExpect(jsonPath("$['paths']['/service-publication-requests/{id}']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_REQUEST_READ"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-requests']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-requests/{id}/approve']['post']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_REQUEST_APPROVE"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-requests/{id}/reject']['post']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_REQUEST_REJECT"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['patch']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE"))
                .andExpect(jsonPath("$['paths']['/service-publication-change-requests/{id}']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-change-requests']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-change-requests/{id}/approve']['post']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-change-requests/{id}/reject']['post']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_READ"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['delete']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_DELETE"))
                .andExpect(jsonPath("$['paths']['/admin/services/{id}']['get']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_ADMIN_READ"))
                .andExpect(jsonPath("$['paths']['/admin/services/{id}']['delete']['x-required-permissions'][0]")
                        .value("PUBLICATIONS_SERVICE_ADMIN_DELETE"))
                .andExpect(jsonPath("$['paths']['/service-publication-requests']['post']['security'][0]['bearerAuth']").exists())
                .andExpect(jsonPath("$['paths']['/service-publication-requests']['post']['requestBody']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/CreateServicePublicationRequestDTO"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['patch']['requestBody']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/UpdateServicePublicationRequestDTO"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['patch']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/services/{id}']['patch']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/services/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['components']['schemas']['CreateServicePublicationRequestDTO']['properties']['images']['minItems']").value(1))
                .andExpect(jsonPath("$['components']['schemas']['CreateServicePublicationRequestDTO']['properties']['images']['maxItems']").value(5))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-requests']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/admin/service-publication-change-requests']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/admin/service-publication-requests']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseAdminServicePublicationRequestResponseDTO"))
                .andExpect(jsonPath("$['paths']['/admin/service-publication-change-requests']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseAdminServicePublicationChangeRequestResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseAdminServicePublicationRequestResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/AdminServicePublicationRequestResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseAdminServicePublicationChangeRequestResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/AdminServicePublicationChangeRequestResponseDTO"))
                .andExpect(jsonPath("$['paths']['/service-publication-requests']['post']['responses']['402']").exists());
    }
}
