package com.jeepclub.backend.authorization.core.application.result.permission;

import com.jeepclub.backend.authorization.core.domain.model.Permission;

import java.util.List;

public record FindAllPermissionsResult(
        List<Permission> permissions
) {
}
