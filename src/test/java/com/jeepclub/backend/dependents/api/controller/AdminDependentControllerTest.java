package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.dependents.api.http.controller.admin.AdminDependentController;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
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
        result = new DependentResult(
                10L, "Pedro Silva", "52998224725",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                null, 5L, DependentStatus.DISABLED, now, now
        );
    }

    @Test
    void listsAndGetsOperationalDependents() throws Exception {
        when(adminDependentService.findAllByUserId(5L)).thenReturn(List.of(result));
        when(adminDependentService.findByUserIdAndId(5L, 10L)).thenReturn(result);

        mockMvc.perform(get("/users/5/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DISABLED"));
        mockMvc.perform(get("/users/5/dependents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }
}
