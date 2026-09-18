package com.jeepclub.backend.iam.identity.core.port;

/**
 * Consumer-owned port used by Identity to check authorization protections
 * before executing administrative lifecycle operations.
 */
public interface UserAuthorizationProtectionPort {

    boolean hasRootRole(Long userId);
}
