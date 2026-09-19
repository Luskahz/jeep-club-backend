package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import com.jeepclub.backend.vehicles.api.http.controller.VehicleController;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestFieldReader;
import com.jeepclub.backend.vehicles.api.http.exceptions.VehicleExceptionHandler;
import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        VehicleEditContractCharacterizationTest.AuthenticationPrincipalTestConfiguration.class,
        EditRequestFieldReader.class,
        VehicleExceptionHandler.class,
        GlobalExceptionHandler.class
})
class VehicleEditContractCharacterizationTest {

    private static final String FULL_EDIT = """
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
    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

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

    @Test
    void completePayloadReachesServiceWithEveryFieldAsPresentValue() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FULL_EDIT))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.of("Trovão"),
                FieldUpdate.of("photo"),
                FieldUpdate.of("ABC1D23"),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(2.0),
                FieldUpdate.of(true)
        )));
    }

    @Test
    void omittedPrimitiveIsTreatedAsPreservingCurrentValueInsteadOfFailing() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withoutField(FULL_EDIT, "manufacturingYear")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), argThatManufacturingYearIsOmitted());
    }

    @Test
    void omittedNullableFieldIsTreatedAsPreservingCurrentValue() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withoutField(FULL_EDIT, "nickname")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.omitted(),
                FieldUpdate.of("photo"),
                FieldUpdate.of("ABC1D23"),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(2.0),
                FieldUpdate.of(true)
        )));
    }

    @Test
    void explicitNullClearsNullableField() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withNullField(FULL_EDIT, "nickname")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.explicitNull(),
                FieldUpdate.of("photo"),
                FieldUpdate.of("ABC1D23"),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(2.0),
                FieldUpdate.of(true)
        )));
    }

    @Test
    void explicitNullOnRequiredFieldIsForwardedAsExplicitNullToTheService() throws Exception {
        // A rejeição de null em campo obrigatório é decidida pelo VehicleEditResolver
        // dentro do service (coberto por VehicleServiceTest/VehicleEditResolverTest);
        // aqui, com o service mockado, só é possível caracterizar que o parsing HTTP
        // distingue corretamente "null explícito" de "ausente" e encaminha ao service.
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withNullField(FULL_EDIT, "plate")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.of("Trovão"),
                FieldUpdate.of("photo"),
                FieldUpdate.explicitNull(),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(2.0),
                FieldUpdate.of(true)
        )));
    }

    @Test
    void explicitFalseOnTowingIsAppliedAndDistinctFromOmission() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withField(FULL_EDIT, "towing", "false")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.of("Trovão"),
                FieldUpdate.of("photo"),
                FieldUpdate.of("ABC1D23"),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(2.0),
                FieldUpdate.of(false)
        )));
    }

    @Test
    void explicitZeroOnEngineDisplacementIsAppliedAndDistinctFromOmission() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withField(FULL_EDIT, "engineDisplacement", "0")))
                .andExpect(status().isNoContent());

        verify(vehicleService).update(eq(42L), eq(7L), eq(new VehicleEditFields(
                FieldUpdate.of("Trovão"),
                FieldUpdate.of("photo"),
                FieldUpdate.of("ABC1D23"),
                FieldUpdate.of("38249206428"),
                FieldUpdate.of("Jeep"),
                FieldUpdate.of("Wrangler"),
                FieldUpdate.of(2023),
                FieldUpdate.of(2024),
                FieldUpdate.of("Verde"),
                FieldUpdate.of(5),
                FieldUpdate.of(FuelType.DIESEL),
                FieldUpdate.of(0.0),
                FieldUpdate.of(true)
        )));
    }

    @Test
    void invalidExplicitZeroOnSeatingCapacityIsRejectedByValidation() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withField(FULL_EDIT, "seatingCapacity", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("seatingCapacity"));
    }

    @Test
    void malformedFieldTypeIsRejectedAsBadRequestInsteadOfFailingUnexpectedly() throws Exception {
        mockMvc.perform(put("/vehicles/edit/member/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withField(FULL_EDIT, "manufacturingYear", "\"not-a-number\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    private VehicleEditFields argThatManufacturingYearIsOmitted() {
        return org.mockito.ArgumentMatchers.argThat(fields ->
                fields != null && fields.manufacturingYear().isOmitted()
                        && fields.nickname().isPresentValue()
        );
    }

    private String withoutField(String json, String field) {
        ObjectNode node = (ObjectNode) jsonMapper.readTree(json);
        node.remove(field);
        return jsonMapper.writeValueAsString(node);
    }

    private String withNullField(String json, String field) {
        ObjectNode node = (ObjectNode) jsonMapper.readTree(json);
        node.putNull(field);
        return jsonMapper.writeValueAsString(node);
    }

    private String withField(String json, String field, String rawValueJson) {
        ObjectNode node = (ObjectNode) jsonMapper.readTree(json);
        node.replace(field, jsonMapper.readTree(rawValueJson));
        return jsonMapper.writeValueAsString(node);
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfiguration implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
