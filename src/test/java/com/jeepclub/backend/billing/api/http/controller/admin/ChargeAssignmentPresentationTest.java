package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.domain.enums.assignment.ChargeAudienceType;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChargeAssignmentPresentationTest {
    private final RolePresentationQuery roles = mock(RolePresentationQuery.class);
    private final ChargeAssignmentPresentation presentation = new ChargeAssignmentPresentation(roles);

    private ChargeAssignmentResult assignment(long id, ChargeAudienceType type, Long roleId) {
        return new ChargeAssignmentResult(id, 1L, type, null, roleId, null, true, Instant.EPOCH, null);
    }
    private void authorize(boolean allowed) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "n/a", allowed ? List.of(new SimpleGrantedAuthority("AUTHORIZATION_ROLE_READ")) : List.of()));
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void resolvesDistinctRolesInOneBatchAndHandlesMissingNames() {
        authorize(true);
        when(roles.findByIds(List.of(2L, 3L))).thenReturn(List.of(new RolePresentationQuery.Details(2L, "Diretoria")));
        var page = presentation.present(new PageImpl<>(List.of(assignment(1, ChargeAudienceType.ROLE, 2L), assignment(2, ChargeAudienceType.ROLE, 3L), assignment(3, ChargeAudienceType.ROLE, 2L), assignment(4, ChargeAudienceType.ALL_MEMBERS, null))));
        assertThat(page.getContent()).extracting("roleName").containsExactly("Diretoria", null, "Diretoria", null);
        assertThat(page.getContent().get(1).roleId()).isEqualTo(3L);
        verify(roles).findByIds(List.of(2L, 3L));
        verifyNoMoreInteractions(roles);
    }
    @Test void hidesRoleAndAvoidsQueryWhenUnauthorizedOrNoRoles() {
        authorize(false);
        var hidden = presentation.present(assignment(1, ChargeAudienceType.ROLE, 2L));
        assertThat(hidden.roleId()).isNull(); assertThat(hidden.roleName()).isNull();
        authorize(true);
        assertThat(presentation.present(new PageImpl<>(List.of(assignment(2, ChargeAudienceType.ALL_MEMBERS, null)))).getContent().get(0).roleName()).isNull();
        assertThat(presentation.present(new PageImpl<>(List.<ChargeAssignmentResult>of())).getContent()).isEmpty();
        verifyNoInteractions(roles);
    }
}
