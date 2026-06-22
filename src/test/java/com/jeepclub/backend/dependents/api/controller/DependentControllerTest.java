package com.jeepclub.backend.dependents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jeepclub.backend.authentication.core.application.service.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.dependents.api.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.service.CreateDependentService;
import com.jeepclub.backend.dependents.core.application.service.DeleteDependentService;
import com.jeepclub.backend.dependents.core.application.service.GetDependentService;
import com.jeepclub.backend.dependents.core.application.service.UpdateDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DependentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DependentControllerTest.AuthenticationPrincipalTestConfiguration.class)
class DependentControllerTest {

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
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    private ObjectMapper objectMapper;
    private Dependent mockDependent;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockDependent = Dependent.reconstitute(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                null,
                true,
                Instant.now(),
                1L, // socioId = 1
                Instant.now(),
                Instant.now()
        );

        UserPrincipal principal = new UserPrincipal(1L, 100L, Instant.now().plusSeconds(3600));
        mockAuth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(mockAuth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sucesso: Criar dependente para o Sócio autenticado")
    void shouldCreateDependentSuccessfully() throws Exception {
        CreateDependentRequestDTO request = new CreateDependentRequestDTO(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                null,
                true
        );

        when(createDependentService.create(
                anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), any(), anyBoolean(), anyLong()
        )).thenReturn(mockDependent);

        mockMvc.perform(post("/dependents")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Pedro Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678900"))
                .andExpect(jsonPath("$.relationshipType").value("CHILD"))
                .andExpect(jsonPath("$.consentAccepted").value(true));
    }

    @Test
    @DisplayName("Falha: Tentar criar dependente sem CPF deve retornar erro de validação")
    void shouldReturnBadRequestWhenCpfIsMissing() throws Exception {
        CreateDependentRequestDTO request = new CreateDependentRequestDTO(
                "Pedro Silva",
                null,
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                null,
                true
        );

        mockMvc.perform(post("/dependents")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sucesso: Listar dependentes do Sócio autenticado")
    void shouldListMyDependentsSuccessfully() throws Exception {
        when(getDependentService.getBySocioId(eq(1L), eq(1L), eq(false)))
                .thenReturn(List.of(mockDependent));

        mockMvc.perform(get("/dependents")
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Pedro Silva"));
    }

    @Test
    @DisplayName("Sucesso: Buscar dependente por ID")
    void shouldGetDependentByIdSuccessfully() throws Exception {
        when(getDependentService.getById(eq(10L), eq(1L), eq(false)))
                .thenReturn(mockDependent);

        mockMvc.perform(get("/dependents/10")
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Pedro Silva"));
    }

    @Test
    @DisplayName("Sucesso: Atualizar dependente")
    void shouldUpdateDependentSuccessfully() throws Exception {
        UpdateDependentRequestDTO request = new UpdateDependentRequestDTO(
                "Pedro Silva Ramos",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11999998888",
                null,
                true
        );

        Dependent updatedDependent = Dependent.reconstitute(
                10L,
                "Pedro Silva Ramos",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11999998888",
                null,
                true,
                Instant.now(),
                1L,
                Instant.now(),
                Instant.now()
        );

        when(updateDependentService.update(
                eq(10L), anyString(), anyString(), any(LocalDate.class),
                any(RelationshipType.class), anyString(), any(), anyBoolean(), eq(1L), eq(false)
        )).thenReturn(updatedDependent);

        mockMvc.perform(put("/dependents/10")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pedro Silva Ramos"))
                .andExpect(jsonPath("$.phoneNumber").value("11999998888"));
    }

    @Test
    @DisplayName("Sucesso: Remover dependente")
    void shouldDeleteDependentSuccessfully() throws Exception {
        doNothing().when(deleteDependentService).delete(eq(10L), eq(1L), eq(false));

        mockMvc.perform(delete("/dependents/10")
                        .principal(mockAuth))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Sucesso: Diretor lista dependentes de qualquer Sócio")
    void shouldAllowDirectorToListDependents() throws Exception {
        when(getDependentService.getBySocioId(eq(5L), any(), eq(true)))
                .thenReturn(List.of(mockDependent));

        mockMvc.perform(get("/socios/5/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pedro Silva"));
    }

    @Test
    @DisplayName("Sucesso: Diretor consulta dependente por ID de qualquer Sócio")
    void shouldAllowDirectorToGetDependentById() throws Exception {
        Dependent dependentOfSocio5 = Dependent.reconstitute(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                null,
                true,
                Instant.now(),
                5L, // Pertence ao socioId = 5
                Instant.now(),
                Instant.now()
        );

        when(getDependentService.getById(eq(10L), any(), eq(true)))
                .thenReturn(dependentOfSocio5);

        mockMvc.perform(get("/socios/5/dependents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Pedro Silva"));
    }

    @Test
    @DisplayName("Falha: Diretor tenta consultar dependente que não pertence ao Sócio informado")
    void shouldFailWhenDependentDoesNotBelongToSocio() throws Exception {
        when(getDependentService.getById(eq(10L), any(), eq(true)))
                .thenReturn(mockDependent); // mockDependent pertence ao socioId = 1, mas requisitamos socioId = 5

        mockMvc.perform(get("/socios/5/dependents/10"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfiguration implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
