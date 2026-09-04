package com.jeepclub.backend.iam.authorization.api.module.role;

import java.util.List;

public interface RoleQuery {

    boolean existsActiveRoleById(Long roleId);

    List<Long> findUserIdsByRoleId(Long roleId);
}
