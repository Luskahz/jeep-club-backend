package com.jeepclub.backend.platform.web.cors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "APP_CORS_ALLOWED_ORIGINS=https://frontend.example.com,https://admin.example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authorizedPreflightPassesSecurityAndUsesConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/authentication/logout")
                        .header("Origin", "https://admin.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://admin.example.com"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void unconfiguredOriginIsRejected() throws Exception {
        mockMvc.perform(options("/authentication/logout")
                        .header("Origin", "https://unconfigured.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
