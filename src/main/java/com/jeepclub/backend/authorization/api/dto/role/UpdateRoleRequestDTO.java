package com.jeepclub.backend.authorization.api.dto.role;

public record UpdateRoleRequestDTO(
        Long roleId,
        String name,
        String description
) {
}
