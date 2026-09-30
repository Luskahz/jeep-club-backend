package com.jeepclub.backend.iam.identity.core.domain.model;

import java.util.Objects;

/** Optional complementary data owned by one Identity user. */
public record UserProfile(Long userId, WorkProfile workProfile, ResidentialAddress address) {
    public UserProfile {
        Objects.requireNonNull(userId, "userId is required");
    }

    public boolean profileCompletionPending() {
        return workProfile == null || !workProfile.isComplete() || address == null || !address.isComplete();
    }

    static String normalize(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
        return normalized;
    }
}
