package com.jeepclub.backend.authentication.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAuthenticationRoutesReachTheirControllersAnonymously() throws Exception {
        assertPublicValidationRoute("/authentication/login", "{}");
        assertPublicValidationRoute("/authentication/register", "{}");
        assertPublicValidationRoute("/authentication/refresh", "{}");
        assertPublicValidationRoute("/authentication/login/password-change", "{}");
    }

    @Test
    void publicRecoveryRoutesAreNotInterceptedBySecurity() throws Exception {
        String cpf = "{\"cpf\":\"52998224725\"}";
        mockMvc.perform(post("/authentication/password-recovery/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpf))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authentication/password-recovery/requests/email-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpf))
                .andExpect(status().isOk());
        assertPublicValidationRoute(
                "/authentication/password-recovery/requests/token/reset",
                "{}"
        );
    }

    @Test
    void authenticatedAndAdministrativeRoutesRemainProtected() throws Exception {
        mockMvc.perform(get("/authentication/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(post("/authentication/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/authentication/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/authentication/admin/password-recovery/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void invalidBearerTokenUsesLocalizedProblemDetail() throws Exception {
        mockMvc.perform(get("/authentication/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.detail").value("The access token is invalid or has expired."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void openApiKeepsPublishedAuthenticationPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/authentication/login']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/register']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/refresh']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/password-recovery/requests']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/admin/users/{userId}/disable']['patch']").exists());
    }

    private void assertPublicValidationRoute(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
