package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Único teste que percorre create → listar → consultar → editar → excluir
 * pela pilha real (HTTP → {@code VehicleService} real → {@code VehicleRepositoryAdapter}
 * real → H2 de teste), sem nenhum mock de service. Os demais testes do
 * módulo cobrem cada etapa isoladamente (unitário de domínio/service, ou MVC
 * com o service mockado); nenhum deles prova que a fiação real do módulo
 * inteiro funciona de ponta a ponta. Não duplica a matriz de campo a campo
 * já coberta por {@code VehicleEditContractCharacterizationTest} nem a
 * autenticação/ownership já coberta por
 * {@code VehicleOwnerAuthenticationIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleLifecycleIntegrationTest {

    private static final String INCLUDE_REQUEST = """
            {
              "nickname": "Trovão",
              "photo": "%s",
              "plate": "LIF1D23",
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
    @Autowired
    private UserRegistration userRegistration;
    @Autowired
    private UserQuery userQuery;
    @Autowired
    private Clock clock;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void createListDetailEditAndDeleteVehicleThroughTheRealStack() throws Exception {
        String bearer = authenticateNewMember();
        MockMultipartFile photo = new MockMultipartFile("file", "vehicle.png", "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10, 1});
        MvcResult uploaded = mockMvc.perform(multipart("/media/images")
                        .file(photo).header(AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storageKey").exists())
                .andReturn();
        String photoKey = objectMapper.readTree(uploaded.getResponse().getContentAsString())
                .get("storageKey").asText();

        mockMvc.perform(post("/vehicles/include/member")
                        .header(AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INCLUDE_REQUEST.formatted(photoKey)))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get("/vehicles/list/member")
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].plate").value("LIF1D23"))
                .andReturn();
        Long vehicleId = extractFirstVehicleId(listResult);

        mockMvc.perform(get("/vehicles/detail/member/" + vehicleId)
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("LIF1D23"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.photo").value(photoKey));

        mockMvc.perform(get("/media/images").param("key", photoKey)
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .containsExactly(photo.getBytes()));

        mockMvc.perform(put("/vehicles/edit/member/" + vehicleId)
                        .header(AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"Apelido Novo\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/vehicles/detail/member/" + vehicleId)
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Apelido Novo"))
                .andExpect(jsonPath("$.plate").value("LIF1D23"));

        mockMvc.perform(delete("/vehicles/delete/member/" + vehicleId)
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/vehicles/detail/member/" + vehicleId)
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/vehicles/list/member")
                        .header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private String authenticateNewMember() {
        Instant now = Instant.now(clock);
        String cpf = "74132146978";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Lifecycle Owner", null, "lifecycle-owner@example.com", cpf,
                        null, null, null, now
                ),
                "lifecycle-password"
        );
        assertThat(userQuery.findByCpf(cpf)).isPresent();
        return "Bearer " + tokens.accessToken();
    }

    private Long extractFirstVehicleId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("content").get(0).get("id").asLong();
    }
}
