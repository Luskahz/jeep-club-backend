package com.jeepclub.backend.dependents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jeepclub.backend.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.dependents.api.http.controller.DependentController;
import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfileData;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DependentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DependentControllerTest.AuthenticationPrincipalTestConfiguration.class)
class DependentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DependentService dependentService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    private ObjectMapper objectMapper;
    private DependentResult dependentResult;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Instant now = Instant.parse("2026-06-30T12:00:00Z");
        Dependent dependent = Dependent.reconstitute(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                now,
                1L,
                now,
                now
        );
        dependentResult = new DependentResult(dependent, null);

        UserPrincipal principal = new UserPrincipal(1L, 100L, now.plusSeconds(3600));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsDependentWithTheExistingContract() throws Exception {
        CreateDependentRequestDTO request = createRequest(null, true);
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class), any(RelationshipType.class),
                anyString(), anyBoolean(), eq(null), anyLong()
        )).thenReturn(dependentResult);

        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.medicalProfile.bloodType").doesNotExist());
    }

    @Test
    void forwardsMedicalProfileDuringCreation() throws Exception {
        MedicalProfileDTO profile = new MedicalProfileDTO(
                "O+", "Dipirona", "Asma", "Aerolin", "Usar bombinha"
        );
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class), any(RelationshipType.class),
                anyString(), anyBoolean(), any(DependentMedicalProfileData.class), anyLong()
        )).thenReturn(new DependentResult(
                dependentResult.dependent(),
                new DependentMedicalProfileData(
                        "O_POSITIVE", "Dipirona", "Asma", "Aerolin", "Usar bombinha"
                )
        ));

        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(profile, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicalProfile.bloodType").value("O_POSITIVE"));
    }

    @Test
    void validatesTheExistingCreateRequest() throws Exception {
        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDependentRequestDTO(
                                "Pedro Silva", null, LocalDate.of(2010, 5, 20),
                                RelationshipType.CHILD, "11988887777", null, true
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsGetsUpdatesAndDeletesUsingTheExistingRoutes() throws Exception {
        when(dependentService.findAllByUserId(1L)).thenReturn(List.of(dependentResult));
        when(dependentService.findById(10L, 1L)).thenReturn(dependentResult);
        when(dependentService.update(
                eq(10L), anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), anyBoolean(), eq(null), eq(1L)
        )).thenReturn(dependentResult);

        mockMvc.perform(get("/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
        mockMvc.perform(get("/dependents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
        mockMvc.perform(put("/dependents/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
        mockMvc.perform(delete("/dependents/10"))
                .andExpect(status().isNoContent());

        verify(dependentService).delete(10L, 1L);
    }

    @Test
    void preservesNotFoundForbiddenAndConflictResponses() throws Exception {
        when(dependentService.findById(10L, 1L))
                .thenThrow(DependentException.notFound())
                .thenThrow(DependentException.accessDenied());
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class), any(RelationshipType.class),
                anyString(), anyBoolean(), eq(null), anyLong()
        )).thenThrow(DependentException.conflict());

        mockMvc.perform(get("/dependents/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPENDENT_NOT_FOUND"));
        mockMvc.perform(get("/dependents/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DEPENDENT_ACCESS_DENIED"));
        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(null, true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPENDENT_CONFLICT"));
    }

    private CreateDependentRequestDTO createRequest(
            MedicalProfileDTO medicalProfile,
            Boolean consent
    ) {
        return new CreateDependentRequestDTO(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                medicalProfile,
                consent
        );
    }

    private UpdateDependentRequestDTO updateRequest() {
        return new UpdateDependentRequestDTO(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                null,
                true
        );
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfiguration implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
