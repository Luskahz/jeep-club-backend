package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import com.jeepclub.backend.vehicles.api.http.controller.VehicleController;
import com.jeepclub.backend.vehicles.api.http.exceptions.VehicleExceptionHandler;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        VehicleEditContractCharacterizationTest.AuthenticationPrincipalTestConfiguration.class,
        VehicleExceptionHandler.class,
        GlobalExceptionHandler.class
})
class VehicleEditContractCharacterizationTest {

    private static final String VALID_EDIT = """
            {
              "nickname": "Trovão",
              "photo": "photo",
              "plate": "ABC1D23",
              "renavam": "38249206428",
              "brand": "Jeep",
              "model": "Wrangler",
              "manufacturingYear": 2023,
              "modelYear": 2024,
              "color": "Verde",
              "seatingCapacity": 5,
              "fuelType": "DIESEL",
              "engineDisplacement": 2.0,
              "towing": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private VehicleService vehicleService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;
    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                7L,
                100L,
                "Test User",
                Instant.parse("2026-09-14T13:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "manufacturingYear",
            "modelYear",
            "seatingCapacity",
            "engineDisplacement",
            "towing"
    })
    void omittedPrimitiveIsRejectedWhileReadingJson(String field) throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withoutField(VALID_EDIT, field)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HTTP_400"));

        verifyNoInteractions(vehicleService);
    }

    @Test
    void completePayloadReachesServiceWithEveryPrimitiveValue() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EDIT))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(
                42L, 7L, "Trovão", "photo", "ABC1D23", "38249206428",
                "Jeep", "Wrangler", 2023, 2024, "Verde", 5,
                com.jeepclub.backend.vehicles.core.domain.enums.FuelType.DIESEL,
                2.0, true
        );
    }

    private String withoutField(String json, String field) throws Exception {
        ObjectNode request = (ObjectNode) jsonMapper.readTree(json);
        request.remove(field);
        return jsonMapper.writeValueAsString(request);
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfiguration implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
