package com.jeepclub.backend.platform.security.membership;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MembershipSecurityCompositionIntegrationTest.TestController.class)
class MembershipSecurityCompositionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MembershipAccessQuery membershipAccessQuery;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/test-membership/only"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(membershipAccessQuery);
    }

    @Test
    void shouldAllowMembershipOnlyEndpointWithoutArtificialPermission() throws Exception {
        authenticate("member", List.of());
        when(membershipAccessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.ALLOWED);

        mockMvc.perform(get("/test-membership/only")
                        .header("Authorization", "Bearer member"))
                .andExpect(status().isOk())
                .andExpect(content().string("membership"));
        verify(membershipAccessQuery).evaluate(42L);
    }

    @Test
    void shouldRequirePermissionInAdditionToMembership() throws Exception {
        authenticate("member", List.of());
        when(membershipAccessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.ALLOWED);

        mockMvc.perform(get("/test-membership/combined")
                        .header("Authorization", "Bearer member"))
                .andExpect(status().isForbidden());
        verify(membershipAccessQuery).evaluate(42L);
    }

    @Test
    void shouldAllowWhenMembershipAndPermissionAreValid() throws Exception {
        authenticate("admin", List.of("MEMBERSHIP_BILLING_CONFIGURATION_READ"));
        when(membershipAccessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.ALLOWED);

        mockMvc.perform(get("/test-membership/combined")
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("combined"));
    }

    @Test
    void shouldNotEvaluateUnannotatedEndpoint() throws Exception {
        authenticate("member", List.of());

        mockMvc.perform(get("/test-membership/plain")
                        .header("Authorization", "Bearer member"))
                .andExpect(status().isOk())
                .andExpect(content().string("plain"));
        verifyNoInteractions(membershipAccessQuery);
    }

    private void authenticate(String token, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token)).thenReturn(
                new JwtAuthenticatedUser(
                        42L,
                        49L,
                        "Member",
                        Instant.parse("2026-09-22T12:00:00Z")
                )
        );
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(42L)).thenReturn(authorities);
    }

    @RestController
    static class TestController {

        @RequiresMembership
        @GetMapping("/test-membership/only")
        String membershipOnly() {
            return "membership";
        }

        @RequiresMembership
        @PreAuthorize("hasAuthority('MEMBERSHIP_BILLING_CONFIGURATION_READ')")
        @GetMapping("/test-membership/combined")
        String combined() {
            return "combined";
        }

        @GetMapping("/test-membership/plain")
        String plain() {
            return "plain";
        }
    }
}
