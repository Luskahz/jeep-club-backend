package com.jeepclub.backend.platform.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"security.frontend-access.enabled=true",
        "security.frontend-access.secret=integration-test-key", "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendAccessIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void loginRequiresFrontendCredential() throws Exception {
        mvc.perform(post("/authentication/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test void frontendCredentialDoesNotReplaceUserJwt() throws Exception {
        mvc.perform(get("/identity/me").header(FrontendAccessFilter.HEADER, "integration-test-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test void corsPreflightDoesNotBypassFrontendAuthentication() throws Exception {
        mvc.perform(options("/identity/me").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test void docsAreUnavailableAndHealthHasNoPublicExemption() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html", "/openapi-custom/index.html"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/actuator/health")).andExpect(status().isForbidden());
    }
}
