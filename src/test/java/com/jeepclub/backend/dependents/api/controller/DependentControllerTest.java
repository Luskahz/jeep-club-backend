package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.dependents.api.http.controller.DependentController;
import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.exception.DependentExceptionHandler;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerInactiveException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DependentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        DependentControllerTest.AuthenticationPrincipalTestConfiguration.class,
        DependentExceptionHandler.class
})
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

    private DependentResult result;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        Instant now = Instant.parse("2026-06-30T12:00:00Z");
        result = new DependentResult(
                10L, "Pedro Silva", "52998224725",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                "11988887777", 1L, DependentStatus.ACTIVE, now, now
        );
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
    void createsListsGetsUpdatesAndDeletesUsingExistingRoutes() throws Exception {
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), anyLong()
        )).thenReturn(result);
        when(dependentService.findAllByUserId(1L)).thenReturn(List.of(result));
        when(dependentService.findById(10L, 1L)).thenReturn(result);
        when(dependentService.update(
                eq(10L), anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), eq(1L)
        )).thenReturn(result);

        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());
        mockMvc.perform(get("/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mockMvc.perform(get("/dependents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
        mockMvc.perform(put("/dependents/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/dependents/10"))
                .andExpect(status().isNoContent());

        verify(dependentService).delete(10L, 1L);
    }

    @Test
    void mapsInactiveOwnerToConflict() throws Exception {
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), anyLong()
        )).thenThrow(new DependentOwnerInactiveException(1L));

        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPENDENT_OWNER_INACTIVE"));
    }

    @Test
    void mapsCpfConflictToConflict() throws Exception {
        when(dependentService.create(
                anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), anyLong()
        )).thenThrow(new DependentCpfAlreadyInUseException());

        mockMvc.perform(post("/dependents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPENDENT_CPF_ALREADY_IN_USE"));
    }

    @Test
    void rejectsInvalidPhoneNumberOnUpdate() throws Exception {
        UpdateDependentRequestDTO request = new UpdateDependentRequestDTO(
                "Pedro Silva", "52998224725", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "123456789"
        );

        mockMvc.perform(put("/dependents/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private CreateDependentRequestDTO createRequest() {
        return new CreateDependentRequestDTO(
                "Pedro Silva", "52998224725", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777"
        );
    }

    private UpdateDependentRequestDTO updateRequest() {
        return new UpdateDependentRequestDTO(
                "Pedro Silva", "52998224725", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777"
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
