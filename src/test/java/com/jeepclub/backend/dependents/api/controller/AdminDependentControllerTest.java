package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.dependents.api.http.controller.admin.AdminDependentController;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDependentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDependentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDependentService adminDependentService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    private DependentResult result;

    @BeforeEach
    void setUp() {
        Instant now = Instant.parse("2026-06-30T12:00:00Z");
        result = new DependentResult(Dependent.reconstitute(
                10L, "Pedro Silva", "12345678900", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777", true, now, 5L, now, now
        ), null);
    }

    @Test
    void preservesAdministrativeListAndDetailRoutes() throws Exception {
        when(adminDependentService.findAllBySocioId(5L)).thenReturn(List.of(result));
        when(adminDependentService.findBySocioIdAndId(5L, 10L)).thenReturn(result);

        mockMvc.perform(get("/socios/5/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
        mockMvc.perform(get("/socios/5/dependents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void preservesBadRequestWhenDependentDoesNotBelongToSocio() throws Exception {
        when(adminDependentService.findBySocioIdAndId(5L, 10L))
                .thenThrow(new IllegalArgumentException(
                        "O dependente informado não pertence ao sócio especificado."
                ));

        mockMvc.perform(get("/socios/5/dependents/10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
