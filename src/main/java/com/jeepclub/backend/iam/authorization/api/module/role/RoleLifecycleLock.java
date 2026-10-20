package com.jeepclub.backend.iam.authorization.api.module.role;
/** Current reads. Caller locks Identity first when checking a user's ROOT protection. */
public interface RoleLifecycleLock {
    boolean isActive(Long roleId);
    boolean hasRootRole(Long userId);
}
