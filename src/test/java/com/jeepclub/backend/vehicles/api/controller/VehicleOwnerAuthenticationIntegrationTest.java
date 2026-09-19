package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleOwnerAuthenticationIntegrationTest {

    private static final String VEHICLE_REQUEST = """
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

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRegistration userRegistration;
    @Autowired private UserQuery userQuery;
    @Autowired private UserAdministration userAdministration;
    @Autowired private Clock clock;
    @MockitoBean private VehicleService vehicleService;

    @Test
    @Transactional
    void onlyAnAdministrativelyActiveAuthenticatedMemberCanCreateOwnVehicle() throws Exception {
        Instant now = Instant.now(clock);
        String cpf = "29568134709";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Vehicle Owner", null, "vehicle-owner@example.com", cpf,
                        null, null, null, now
                ),
                "vehicle-password"
        );
        Long userId = userQuery.findByCpf(cpf).orElseThrow().id();
        String bearer = "Bearer " + tokens.accessToken();

        mockMvc.perform(post("/vehicles/include/member")
                        .header(AUTHORIZATION, bearer)
                        .contentType(APPLICATION_JSON)
                        .content(VEHICLE_REQUEST))
                .andExpect(status().isCreated());

        verify(vehicleService).create(
                eq("Trovão"), eq("photo"), eq("ABC1D23"), eq("38249206428"),
                eq("Jeep"), eq("Wrangler"), eq(2023), eq(2024), eq("Verde"), eq(5),
                eq(com.jeepclub.backend.vehicles.core.domain.enums.FuelType.DIESEL), eq(2.0),
                eq(true), eq(userId)
        );

        userAdministration.disable(userId, Instant.now(clock));

        mockMvc.perform(post("/vehicles/include/member")
                        .header(AUTHORIZATION, bearer)
                        .contentType(APPLICATION_JSON)
                        .content(VEHICLE_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));

        verifyNoMoreInteractions(vehicleService);
    }
}
