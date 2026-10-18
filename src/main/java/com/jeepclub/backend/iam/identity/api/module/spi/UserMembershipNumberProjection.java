package com.jeepclub.backend.iam.identity.api.module.spi;
import java.util.*;
/** Presentation SPI. Identity never stores or derives a membership number. */
public interface UserMembershipNumberProjection {
    String currentNumber(Long identityId);
    /** Exact current business number, never an historical claim. */
    Optional<Long> identityIdByCurrentNumber(String membershipNumber);
    Map<Long,String> currentNumbers(Collection<Long> identityIds);
}
