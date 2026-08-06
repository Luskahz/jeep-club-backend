package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.dependents.core.application.service.CreateDependentService;
import com.jeepclub.backend.dependents.core.application.service.DeleteDependentService;
import com.jeepclub.backend.dependents.core.application.service.GetDependentService;
import com.jeepclub.backend.dependents.core.application.service.UpdateDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.health.core.application.MedicalProfileService;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DependentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDependentService createDependentService;
    @MockitoBean
    private UpdateDependentService updateDependentService;
    @MockitoBean
    private DeleteDependentService deleteDependentService;
    @MockitoBean
    private GetDependentService getDependentService;
    @MockitoBean
    private MedicalProfileService medicalProfileService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void unauthenticatedRequestIsRejectedBySecurityFilterChain() throws Exception {
        mockMvc.perform(get("/dependents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedTitularCanListOwnDependents() throws Exception {
        authenticate("titular-token", 1L, List.of());
        when(getDependentService.getBySocioId(1L, 1L, false))
                .thenReturn(List.of(dependent(10L, 1L)));
        when(medicalProfileService.getByOwner(eq(MedicalProfileOwnerType.DEPENDENT), eq(10L)))
                .thenThrow(new MedicalProfileNotFoundException());

        mockMvc.perform(get("/dependents")
                        .header("Authorization", "Bearer titular-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void adminAuthorityCanListDependentsBySocio() throws Exception {
        authenticate("admin-token", 99L, List.of("DEPENDENTS_DEPENDENT_READ"));
        when(getDependentService.getBySocioId(1L, null, true))
                .thenReturn(List.of(dependent(10L, 1L)));
        when(medicalProfileService.getByOwner(eq(MedicalProfileOwnerType.DEPENDENT), eq(10L)))
                .thenThrow(new MedicalProfileNotFoundException());

        mockMvc.perform(get("/socios/1/dependents")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].socioId").value(1L));
    }

    @Test
    void authenticatedUserWithoutAdminAuthorityCannotListDependentsBySocio() throws Exception {
        authenticate("member-token", 2L, List.of());

        mockMvc.perform(get("/socios/1/dependents")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden());
    }

    private void authenticate(String token, Long userId, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token))
                .thenReturn(new JwtAuthenticatedUser(userId, 100L + userId, Instant.now().plusSeconds(3600)));
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(userId))
                .thenReturn(authorities);
    }

    private Dependent dependent(Long id, Long socioId) {
        Instant now = Instant.parse("2026-06-30T12:00:00Z");
        return Dependent.reconstitute(
                id,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                now,
                socioId,
                now,
                now
        );
    }
}
