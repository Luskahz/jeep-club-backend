package com.jeepclub.backend.iam.authorization.core.application.result;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;

import java.util.List;

public record RolesResult(
        List<Role> roles
) {
}
