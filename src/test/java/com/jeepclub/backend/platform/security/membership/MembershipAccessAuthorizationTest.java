package com.jeepclub.backend.platform.security.membership;

import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.memberships.api.module.exception.MembershipChargeUnavailableException;
import com.jeepclub.backend.memberships.api.module.exception.MembershipPaymentRequiredException;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipAccessAuthorizationTest {

    @Mock
    private MembershipAccessQuery accessQuery;

    @Test
    void shouldDelegateAuthenticatedUserIdAndAllow() {
        when(accessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.ALLOWED);
        MembershipAccessAuthorization authorization = new MembershipAccessAuthorization(accessQuery);

        assertThat(authorization.check(authentication())).isTrue();
    }

    @Test
    void shouldPreserveMembershipFailureSemantics() {
        MembershipAccessAuthorization authorization = new MembershipAccessAuthorization(accessQuery);

        when(accessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.PAYMENT_REQUIRED);
        assertThatThrownBy(() -> authorization.check(authentication()))
                .isInstanceOf(MembershipPaymentRequiredException.class);

        when(accessQuery.evaluate(42L)).thenReturn(MembershipAccessResult.CHARGE_NOT_FOUND);
        assertThatThrownBy(() -> authorization.check(authentication()))
                .isInstanceOf(MembershipChargeUnavailableException.class);
    }

    @Test
    void shouldRejectMissingAuthentication() {
        MembershipAccessAuthorization authorization = new MembershipAccessAuthorization(accessQuery);

        assertThatThrownBy(() -> authorization.check(null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        UserPrincipal principal = new UserPrincipal(
                42L,
                7L,
                "Member",
                Instant.parse("2026-09-21T13:00:00Z")
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
