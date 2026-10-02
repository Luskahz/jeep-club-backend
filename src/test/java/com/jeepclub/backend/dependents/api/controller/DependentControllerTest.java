package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.dependents.api.http.controller.DependentController;
import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.exception.DependentExceptionHandler;
import com.jeepclub.backend.dependents.core.application.exception.DependentAccessDeniedException;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerInactiveException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerNotFoundException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentAlreadyDeletedException;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

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
        UserPrincipal principal = new UserPrincipal(1L, 100L, "Test User", now.plusSeconds(3600));
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

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void validatesCreateAndUpdateBeforeCallingService(String method, String field, String value) throws Exception {
        var body = jsonMapper.createObjectNode();
        body.put("name", "Pedro");
        body.put("cpf", "52998224725");
        body.put("birthDate", "2010-05-20");
        body.put("relationshipType", "CHILD");
        body.set(field, jsonMapper.readTree(value));
        mockMvc.perform(request(HttpMethod.valueOf(method),
                        method.equals("POST") ? "/dependents" : "/dependents/10")
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").exists());
        Mockito.verifyNoInteractions(dependentService);
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of(
                new String[]{"name", "\" \""},
                new String[]{"cpf", "\"11111111111\""},
                new String[]{"birthDate", "null"},
                new String[]{"birthDate", "\"9999-01-01\""},
                new String[]{"relationshipType", "null"},
                new String[]{"phoneNumber", "\"(11) 99999-9999\""}
        ).map(values -> Arguments.of(method, values[0], values[1])));
    }

    @ParameterizedTest
    @MethodSource("lookupErrors")
    void mapsLookupFailuresToProblemDetails(RuntimeException failure, int status, String code) throws Exception {
        when(dependentService.findById(10L, 1L)).thenThrow(failure);
        mockMvc.perform(get("/dependents/10"))
                .andExpect(status().is(status))
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    static Stream<Arguments> lookupErrors() {
        return Stream.of(
                Arguments.of(
                        new DependentNotFoundException(10L),
                        404, "DEPENDENT_NOT_FOUND"),
                Arguments.of(
                        new DependentAccessDeniedException(10L),
                        403, "DEPENDENT_ACCESS_DENIED"));
    }

    @Test
    void missingOwnerIsNotFoundAndRepeatedDeleteIsConflict() throws Exception {
        when(dependentService.create(anyString(), anyString(), any(), any(), any(), eq(1L)))
                .thenThrow(new DependentOwnerNotFoundException(1L));
        mockMvc.perform(post("/dependents").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPENDENT_OWNER_NOT_FOUND"));
        Mockito.doThrow(new DependentAlreadyDeletedException(10L))
                .when(dependentService).delete(10L, 1L);
        mockMvc.perform(delete("/dependents/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPENDENT_ALREADY_DELETED"));
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
