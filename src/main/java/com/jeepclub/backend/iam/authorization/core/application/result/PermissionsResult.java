package com.jeepclub.backend.iam.authorization.core.application.result;

import com.jeepclub.backend.iam.authorization.core.domain.model.Permission;

import java.util.List;

public record PermissionsResult(
        List<Permission> permissions
) {
}
