package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.core.application.result.BirthdayResult;

public record BirthdayItemDTO(Long userId, String name, Integer birthDay, Integer birthMonth) {
    public static BirthdayItemDTO from(BirthdayResult result) {
        return new BirthdayItemDTO(result.userId(), result.name(), result.birthDay(), result.birthMonth());
    }
}
