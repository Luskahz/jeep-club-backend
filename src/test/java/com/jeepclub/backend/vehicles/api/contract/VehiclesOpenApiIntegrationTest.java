package com.jeepclub.backend.vehicles.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehiclesOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDescribesMemberAndAdministrativeVehicleContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/vehicles/include/member']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseListResponseDTO"))
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'page')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'size')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'sort')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'page')].schema.default")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'size')].schema.default")
                        .value(hasItem(10)))
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['parameters'][?(@.name == 'sort')].schema.default[0]")
                        .value(hasItem("id,ASC")))
                .andExpect(jsonPath("$['paths']['/vehicles/list/member']['get']['description']")
                        .value(org.hamcrest.Matchers.containsString("limitado globalmente a 50")))
                .andExpect(jsonPath("$['paths']['/vehicles/detail/member/{vehicleId}']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/DetailResponseDTO"))
                .andExpect(jsonPath("$['paths']['/vehicles/detail-for-edit/member/{vehicleId}']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/DetailForEditResponseDTO"))
                .andExpect(jsonPath("$['paths']['/vehicles/edit/member/{vehicleId}']['put']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/vehicles/delete/member/{vehicleId}']['delete']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/vehicles/include/admin/{memberId}']['post']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_CREATE"))
                .andExpect(jsonPath("$['paths']['/vehicles/list/admin']['get']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_READ"))
                .andExpect(jsonPath("$['paths']['/vehicles/list/admin']['get']['parameters'][?(@.name == 'page')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/list/admin']['get']['parameters'][?(@.name == 'size')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/list/admin']['get']['parameters'][?(@.name == 'sort')]").isArray())
                .andExpect(jsonPath("$['paths']['/vehicles/detail/admin/{vehicleId}']['get']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_READ"))
                .andExpect(jsonPath("$['paths']['/vehicles/detail-for-edit/admin/{vehicleId}']['get']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_READ"))
                .andExpect(jsonPath("$['paths']['/vehicles/edit/admin/{vehicleId}']['put']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_UPDATE"))
                .andExpect(jsonPath("$['paths']['/vehicles/delete/admin/{vehicleId}']['delete']['x-required-permissions'][0]")
                        .value("VEHICLES_VEHICLE_DELETE"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseListResponseDTO']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseListResponseDTO']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/ListResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseListResponseDTO']['properties']['totalPages']['format']")
                        .value("int32"))
                .andExpect(jsonPath("$['components']['schemas']['EditRequestDTO']['description']")
                        .value(org.hamcrest.Matchers.containsString("Um campo omitido preserva o valor atual")))
                .andExpect(jsonPath("$['components']['schemas']['EditRequestDTO']['required']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['EditRequestDTO']['properties']['engineDisplacement']['description']")
                        .value(org.hamcrest.Matchers.containsString("Zero é um valor válido")))
                .andExpect(jsonPath("$['components']['schemas']['EditRequestDTO']['properties']['towing']['description']")
                        .value(org.hamcrest.Matchers.containsString("null explícito é rejeitado")))
                .andExpect(jsonPath("$['components']['schemas']['DetailResponseDTO']['properties']['status']['enum']")
                        .value(hasItem("ACTIVE")))
                .andExpect(jsonPath("$['components']['schemas']['DetailResponseDTO']['properties']['status']['enum']")
                        .value(org.hamcrest.Matchers.not(hasItem("SOFT_DELETED"))))
                .andExpect(jsonPath("$['components']['schemas']['DetailResponseDTO']['properties']['disabledAt']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/vehicles/edit/admin/{vehicleId}']['put']['responses']['409']['content']['application/json']")
                        .doesNotExist());
    }
}
