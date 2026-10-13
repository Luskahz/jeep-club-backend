package com.jeepclub.backend.iam.identity.api.module.spi;

import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import java.time.Instant;

/** Consumer-owned boundary: Authentication owns credentials and invitations. */
public interface AdminUserCredentialProvisioningPort {
    String provision(Long userId, UserActivationMethod method, Instant now);
    String invitationStatus(Long userId);
}
