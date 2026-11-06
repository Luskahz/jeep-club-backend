package com.jeepclub.backend.storage.core.domain.model;

import java.time.Instant;
import java.util.Objects;

final class MediaInvariants {
    static void require(boolean valid, String field) {
        if (!valid) throw new IllegalArgumentException("Invalid media " + field);
    }
    static void id(Long value, String field) { require(value == null || value > 0, field); }
    static void requiredId(Long value, String field) { require(value != null && value > 0, field); }
    static void version(Long value) { require(value == null || value >= 0, "version"); }
    static void token(String value, int max, String field) {
        require(value != null && value.length() <= max && value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*"), field);
    }
    static void optionalToken(String value, int max, String field) { if (value != null) token(value, max, field); }
    static void dates(Instant created, Instant updated) {
        Objects.requireNonNull(created, "createdAt"); Objects.requireNonNull(updated, "updatedAt");
        require(!updated.isBefore(created), "timestamps");
    }
    private MediaInvariants() {}
}
