package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.*;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.core.domain.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminVehicleSearchHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired UserQuery identities;
    @MockitoBean JwtTokenParser jwt;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;
    @BeforeEach void auth() {
        when(jwt.parseAndValidate("search-token")).thenReturn(new JwtAuthenticatedUser(99L,199L,"Admin",Instant.now().plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(99L)).thenReturn(List.of("VEHICLES_VEHICLE_READ"));
    }
    @Test void ownerNameIsAppliedBeforePageCountAndSelectedFieldsDoNotLeakAuxiliaryId() throws Exception {
        long owner=owner("Literal %_! Owner");
        var first=vehicle(owner,"Search","ABC1D23","Trail");
        var second=vehicle(owner,"Search","DEF4G56","Trail");
        vehicle(owner("Other"),"Search","GHI7J89","Trail");
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token")
                .param("q"," %_! ").param("nickname","search").param("model","TRAIL")
                .param("size","1").param("page","1").param("sort","nickname,asc").param("fields","ID,OWNER_NAME"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].ownerName").value("Literal %_! Owner"))
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist()).andExpect(jsonPath("$.content[0].plate").doesNotExist());
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("ownerId",Long.toString(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(first.getId()))
                .andExpect(jsonPath("$.content[0].modelYear").value(2020)).andExpect(jsonPath("$.content[0].ownerName").value("Literal %_! Owner"));
        assertThat(identities.findIdsByName(" %_! ")).containsExactly(owner);
    }
    @Test void qUsesOrAndPlatesIgnoreMaskWhileFiltersUseAnd() throws Exception {
        long owner=owner("Plate Owner");var row=vehicle(owner,"Nick!%_","ABC1D23","Special Model");
        for(String q:List.of("abc-1d23","ABC.1D23","!%_","special model","Plate Owner")) {
            mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("q",q))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(row.getId())).andExpect(jsonPath("$.totalElements").value(1));
        }
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("plate"," abc-1d23 ")
                .param("ownerId",Long.toString(owner)).param("fields","PLATE,PHOTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].plate").value("ABC1D23"))
                .andExpect(jsonPath("$.content[0].photo").isEmpty()).andExpect(jsonPath("$.content[0].ownerName").doesNotExist());
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("q","Plate Owner").param("model","absent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        row.setStatus(VehicleStatus.INACTIVE);em.flush();
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("q","   "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("status","INACTIVE").param("page","1").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content").isEmpty());
        row.setLegacyDeleted(true);em.flush();
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("status","INACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void ownerCardinalityNeverSilentlyTruncatesAndInvalidParametersAre400() throws Exception {
        for(int i=0;i<501;i++) owner("Broad Synthetic Owner");
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param("q","Broad Synthetic"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VEHICLE_INVALID_SEARCH"));
        for(var invalid:Map.of("fields","RENAVAM","sort","ownerName,asc","ownerId","0","status","INVALID","q","x".repeat(151),"page","-1","size","0","plate","---").entrySet()) {
            mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token").param(invalid.getKey(),invalid.getValue()))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        mvc.perform(get("/vehicles/list/admin")).andExpect(status().isUnauthorized());
        when(authorities.findAuthorityCodesByUserId(99L)).thenReturn(List.of());
        mvc.perform(get("/vehicles/list/admin").header("Authorization","Bearer search-token")).andExpect(status().isForbidden());
    }
    private long owner(String name) {
        var row=new UserEntity();row.setName(name);row.setCpf(String.format("%011d",Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000000L)));
        row.setStatus(UserStatus.ACTIVE);row.setCreatedAt(Instant.now());em.persist(row);return row.getId();
    }
    private VehicleEntity vehicle(long owner,String nickname,String plate,String model) {
        var row=new VehicleEntity();row.setOwnerId(owner);row.setNickname(nickname);row.setPlate(plate);
        row.setRenavam(String.format("%011d",Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000000L)));
        row.setBrand("Jeep");row.setModel(model);row.setModelYear(2020);row.setManufacturingYear(2020);row.setSeatingCapacity(5);
        row.setFuelType(FuelType.GASOLINE);row.setEngineDisplacement(2.0);row.setTowing(false);row.setStatus(VehicleStatus.ACTIVE);
        em.persist(row);em.flush();return row;
    }
}
