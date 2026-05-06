package com.jeepclub.backend.authorization.core.application.result;

import com.jeepclub.backend.authorization.core.domain.model.Permission;

import java.util.List;

public record PermissionsResult(
        List<Permission> permissions
) {
}
