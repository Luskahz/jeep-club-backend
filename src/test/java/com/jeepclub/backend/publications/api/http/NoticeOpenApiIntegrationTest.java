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
class NoticeOpenApiIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test void noticeContractDescribesPathsSchemasSecurityAndDistinctPermissions() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/admin/notices']['post']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_CREATE"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['get']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_READ"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['patch']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_UPDATE"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}/publish']['post']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_PUBLISH"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}/archive']['post']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_ARCHIVE"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['delete']['x-required-permissions'][0]").value("PUBLICATIONS_NOTICE_DELETE"))
                .andExpect(jsonPath("$['paths']['/admin/notices']['post']['security'][0]['bearerAuth']").exists())
                .andExpect(jsonPath("$['paths']['/admin/notices']['post']['requestBody']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/CreateNoticeRequestDTO"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['patch']['requestBody']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/UpdateNoticeRequestDTO"))
                .andExpect(jsonPath("$['paths']['/admin/notices']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['patch']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/admin/notices/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['components']['schemas']['CreateNoticeRequestDTO']['properties']['images']['minItems']").value(1))
                .andExpect(jsonPath("$['components']['schemas']['CreateNoticeRequestDTO']['properties']['images']['maxItems']").value(5))
                .andExpect(jsonPath("$['components']['schemas']['NoticeResponseDTO']['properties']['status']['enum']").isArray())
                .andExpect(jsonPath("$['paths']['/publications']").doesNotExist());
    }
}
