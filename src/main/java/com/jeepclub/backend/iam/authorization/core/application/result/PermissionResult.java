package com.jeepclub.backend.iam.authorization.core.application.result;

import com.jeepclub.backend.iam.authorization.core.domain.model.Permission;

public record PermissionResult(
        Permission permission
) {
}