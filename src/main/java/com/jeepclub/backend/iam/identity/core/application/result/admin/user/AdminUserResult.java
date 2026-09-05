package com.jeepclub.backend.iam.identity.core.application.result.admin.user;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record AdminUserResult(
        Long id,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        String profilePhotoUrl,
        UserStatus status,
        Instant createdAt,
        Instant disabledAt,
        Instant updatedAt
) {
    public static AdminUserResult from(UserDetails user) {
        Objects.requireNonNull(user, "user cannot be null");
        return new AdminUserResult(
                user.id(), user.name(), user.birthDate(), user.email(), user.cpf(),
                user.rg(), user.phoneNumber(), user.profilePhotoUrl(),
                user.administrativelyActive() ? UserStatus.ACTIVE : UserStatus.DISABLED,
                user.createdAt(), user.disabledAt(), user.updatedAt()
        );
    }
}
