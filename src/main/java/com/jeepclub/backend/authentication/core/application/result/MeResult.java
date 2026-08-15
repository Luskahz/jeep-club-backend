package com.jeepclub.backend.authentication.core.application.result;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MeResult(
        Long userId,
        String userName,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        String profilePhotoUrl,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant updatedAt,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}