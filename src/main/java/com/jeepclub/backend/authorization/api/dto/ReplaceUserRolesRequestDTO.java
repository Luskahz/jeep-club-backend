package com.jeepclub.backend.authorization.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ReplaceUserRolesRequestDTO(
        @NotNull(message = "roleIds cannot be null")
        List<@Positive(message = "roleId must be positive") Long> roleIds
) {
}