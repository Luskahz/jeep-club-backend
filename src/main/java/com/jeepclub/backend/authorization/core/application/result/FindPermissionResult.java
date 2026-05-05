package com.jeepclub.backend.authorization.core.application.result;

import com.jeepclub.backend.authorization.core.domain.model.Permission;

public record FindPermissionResult(
        Permission permission
) {
}
