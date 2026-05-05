package com.jeepclub.backend.authorization.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RolePermission {
    Long idRole;
    Long IdPermission;
    Instant createdAt;
    Instant updatedAt;
}
