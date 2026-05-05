package com.jeepclub.backend.authorization.core.domain.model;

import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Role {
    Long id;
    String name;
    String description;
    RoleStatus status;
    Instant createdAt;
    Instant updatedAt;
    Instant deletedAt;
}
