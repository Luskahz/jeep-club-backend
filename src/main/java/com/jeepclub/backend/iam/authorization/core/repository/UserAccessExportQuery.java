package com.jeepclub.backend.iam.authorization.core.repository;
import java.util.*;
import java.time.Instant;
public interface UserAccessExportQuery {
    record RoleAssignment(Long userId, Long roleId, String roleName, String description, String kind, String status, Instant assignedAt) {}
    record EffectivePermission(Long userId, String module, String code) {}
    List<RoleAssignment> roles(Collection<Long> userIds);
    List<EffectivePermission> permissions(Collection<Long> userIds);
}
