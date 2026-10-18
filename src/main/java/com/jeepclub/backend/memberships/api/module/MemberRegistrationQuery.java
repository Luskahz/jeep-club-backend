package com.jeepclub.backend.memberships.api.module;
import java.util.*;
/** Current values only. No cache, no fallback to technical IDs. Batch limit: 500 IDs. */
public interface MemberRegistrationQuery {
    Optional<MemberRegistration> byIdentityUserId(Long identityUserId);
    Map<Long,String> currentNumbers(Collection<Long> identityUserIds);
    Optional<MemberRegistration> byCurrentMembershipNumber(String membershipNumber);
}
