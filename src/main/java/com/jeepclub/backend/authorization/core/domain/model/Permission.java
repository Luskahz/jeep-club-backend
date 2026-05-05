package com.jeepclub.backend.authorization.core.domain.model;

import com.jeepclub.backend.authorization.core.domain.enums.ModuleCode;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Permission {
    Long id;
    PermissionCode code;
    String description;
    ModuleCode module;
    Instant createdAt;
    Instant updatedAt;

}
