package com.jeepclub.backend.authorization.core.application.result.role;

import com.jeepclub.backend.authorization.core.domain.model.Role;

import java.util.List;

public record FindAllRolesResult(
        List<Role> roles
) {
}
