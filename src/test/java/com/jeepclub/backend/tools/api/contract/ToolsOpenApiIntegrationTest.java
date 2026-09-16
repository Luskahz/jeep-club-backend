package com.jeepclub.backend.tools.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolsOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDescribesCurrentMemberAndAdministrativeToolContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/tools']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseToolSummaryResponseDTO"))
                .andExpect(jsonPath("$['paths']['/tools']['get']['description']")
                        .value(org.hamcrest.Matchers.containsString("ACTIVE e INACTIVE")))
                .andExpect(jsonPath("$['paths']['/tools']['get']['parameters'][?(@.name == 'page')]").isArray())
                .andExpect(jsonPath("$['paths']['/tools']['get']['parameters'][?(@.name == 'size')]").isArray())
                .andExpect(jsonPath("$['paths']['/tools']['get']['parameters'][?(@.name == 'sort')]").isArray())
                .andExpect(jsonPath("$['paths']['/tools']['get']['parameters'][?(@.name == 'page')].schema.default")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$['paths']['/tools']['get']['parameters'][?(@.name == 'size')].schema.default")
                        .value(hasItem(20)))
                .andExpect(jsonPath("$['paths']['/tools/{id}']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/tools/{id}/activate']['patch']['responses']['409']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/tools/{id}/deactivate']['patch']['responses']['409']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/tools/{id}']['put']['description']")
                        .value(org.hamcrest.Matchers.containsString("campos não nulos")))
                .andExpect(jsonPath("$['paths']['/admin/tools']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseAdminToolSummaryResponseDTO"))
                .andExpect(jsonPath("$['paths']['/admin/tools']['get']['parameters'][?(@.name == 'name')]").isArray())
                .andExpect(jsonPath("$['paths']['/admin/tools']['get']['parameters'][?(@.name == 'status')]").isArray())
                .andExpect(jsonPath("$['paths']['/admin/tools']['get']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_READ"))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}']['get']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_READ"))
                .andExpect(jsonPath("$['paths']['/admin/tools/users/{userId}']['post']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_CREATE"))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}']['put']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_UPDATE"))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}/activate']['patch']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_ACTIVATE"))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}/deactivate']['patch']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_DEACTIVATE"))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}']['delete']['x-required-permissions'][0]")
                        .value("TOOLS_TOOL_DELETE"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseToolSummaryResponseDTO']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseToolSummaryResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/ToolSummaryResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseAdminToolSummaryResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/AdminToolSummaryResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseToolSummaryResponseDTO']['properties']['sort']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['ToolResponseDTO']['properties']['status']['enum']")
                        .value(hasItems("ACTIVE", "INACTIVE")))
                .andExpect(jsonPath("$['components']['schemas']['ToolResponseDTO']['properties']['status']['enum'].length()")
                        .value(2))
                .andExpect(jsonPath("$['paths']['/admin/tools/{id}']['delete']['responses']['409']['content']['application/json']")
                        .doesNotExist());
    }
}
