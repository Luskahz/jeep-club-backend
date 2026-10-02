package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.AdminVehicleService;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, com a cadeia real de segurança carregada (não {@code addFilters = false}
 * nem contexto fatiado), que as rotas administrativas de Vehicles exigem
 * autenticação e a authority específica de cada ação. Antes desta Story, a
 * única evidência de {@code @PreAuthorize}/{@code @RequiredPermission} nas
 * rotas admin era a declaração estática no OpenAPI
 * ({@link com.jeepclub.backend.vehicles.api.contract.VehiclesOpenApiIntegrationTest}),
 * nunca a aplicação real em runtime. Segue o mesmo padrão já estabelecido em
 * {@code DependentSecurityIntegrationTest}/{@code PaymentReceiptSecurityIntegrationTest}:
 * autenticação simulada via mocks de {@link JwtTokenParser}/
 * {@link UserAuthoritiesProvider}, sem inventar mecanismo de teste novo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleAdminSecurityIntegrationTest {

    private static final String INCLUDE_REQUEST = """
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
    private AdminVehicleService adminVehicleService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void unauthenticatedRequestToAdminCreateIsRejected() throws Exception {
        mockMvc.perform(post("/vehicles/include/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INCLUDE_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberWithoutCreateAuthorityCannotIncludeVehicleForAnotherOwner() throws Exception {
        authenticate("member-token", 2L, List.of());

        mockMvc.perform(post("/vehicles/include/admin/1")
                        .header("Authorization", "Bearer member-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INCLUDE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminWithCreateAuthorityCanIncludeVehicleForAnotherOwner() throws Exception {
        authenticate("admin-token", 99L, List.of("VEHICLES_VEHICLE_CREATE"));

        mockMvc.perform(post("/vehicles/include/admin/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INCLUDE_REQUEST))
                .andExpect(status().isCreated());

        verify(adminVehicleService).createForOwner(
                "Trovão", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Verde", 5,
                com.jeepclub.backend.vehicles.core.domain.enums.FuelType.DIESEL,
                2.0, true, 1L
        );
    }

    @Test
    void readOnlyAdminCannotIncludeVehicle() throws Exception {
        // A authority READ não deve satisfazer @PreAuthorize da rota CREATE:
        // cada ação administrativa exige a permission específica, não "ser admin" em geral.
        authenticate("read-only-admin-token", 98L, List.of("VEHICLES_VEHICLE_READ"));

        mockMvc.perform(post("/vehicles/include/admin/1")
                        .header("Authorization", "Bearer read-only-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INCLUDE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void memberWithoutReadAuthorityCannotListAdminVehicles() throws Exception {
        authenticate("member-token", 2L, List.of());

        mockMvc.perform(get("/vehicles/list/admin")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminWithReadAuthorityCanListAdminVehicles() throws Exception {
        authenticate("admin-token", 99L, List.of("VEHICLES_VEHICLE_READ"));
        Page<com.jeepclub.backend.vehicles.core.domain.model.Vehicle> emptyPage =
                new PageImpl<>(List.of());
        when(adminVehicleService.findAll(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/vehicles/list/admin")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void memberWithoutUpdateAuthorityCannotEditAdminVehicle() throws Exception {
        authenticate("member-token", 2L, List.of());

        mockMvc.perform(put("/vehicles/edit/admin/1")
                        .header("Authorization", "Bearer member-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminWithUpdateAuthorityCanEditAdminVehicle() throws Exception {
        authenticate("admin-token", 99L, List.of("VEHICLES_VEHICLE_UPDATE"));

        mockMvc.perform(put("/vehicles/edit/admin/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void memberWithoutDeleteAuthorityCannotDeleteAdminVehicle() throws Exception {
        authenticate("member-token", 2L, List.of());

        mockMvc.perform(delete("/vehicles/delete/admin/1")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminWithDeleteAuthorityCanDeleteAdminVehicle() throws Exception {
        authenticate("admin-token", 99L, List.of("VEHICLES_VEHICLE_DELETE"));

        mockMvc.perform(delete("/vehicles/delete/admin/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(adminVehicleService).delete(1L, 99L);
    }

    private void authenticate(String token, Long userId, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token)).thenReturn(
                new JwtAuthenticatedUser(userId, 100L + userId, "Test Admin", Instant.now().plusSeconds(3600))
        );
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(userId))
                .thenReturn(authorities);
    }
}
