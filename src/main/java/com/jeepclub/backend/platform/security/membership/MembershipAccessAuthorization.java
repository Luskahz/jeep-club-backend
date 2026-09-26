package com.jeepclub.backend.platform.security.membership;

import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.memberships.api.module.exception.MembershipChargeUnavailableException;
import com.jeepclub.backend.memberships.api.module.exception.MembershipPaymentRequiredException;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("membershipAccessAuthorization")
@RequiredArgsConstructor
public class MembershipAccessAuthorization {

    private final MembershipAccessQuery membershipAccessQuery;

    public boolean check(Authentication authentication) {
        UserPrincipal principal = authenticatedPrincipal(authentication);
        MembershipAccessResult result = membershipAccessQuery.evaluate(principal.getUserId());

        return switch (result) {
            case ALLOWED -> true;
            case PAYMENT_REQUIRED -> throw new MembershipPaymentRequiredException();
            case CHARGE_NOT_FOUND -> throw new MembershipChargeUnavailableException();
        };
    }

    private static UserPrincipal authenticatedPrincipal(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required.");
        }
        return principal;
    }
}
