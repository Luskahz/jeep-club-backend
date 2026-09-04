package com.jeepclub.backend.identity.core.domain.model;

import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.domain.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.identity.core.domain.exception.UserNotDisabledException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_EMAIL_LENGTH = 180;
    private static final int MAX_RG_LENGTH = 20;
    private static final int MAX_PHONE_NUMBER_LENGTH = 20;
    private static final int MAX_PROFILE_PHOTO_URL_LENGTH = 255;

    private Long id;
    private String name;
    private LocalDate birthDate;
    private String email;
    private String cpf;
    private String rg;
    private String phoneNumber;
    private String profilePhotoUrl;
    private UserStatus status;
    private Instant createdAt;
    private Instant disabledAt;
    private Instant updatedAt;

    public static User create(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String phoneNumber,
            String profilePhotoUrl,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        User user = new User();
        user.name = normalizeName(name);
        user.birthDate = birthDate;
        user.email = normalizeEmail(email);
        user.cpf = normalizeCpf(cpf);
        user.rg = normalizeRg(rg);
        user.phoneNumber = normalizePhoneNumber(phoneNumber);
        user.profilePhotoUrl = normalizeProfilePhotoUrl(profilePhotoUrl);
        user.status = UserStatus.ACTIVE;
        user.createdAt = now;
        return user;
    }

    public static User reconstitute(
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
        validateId(id);
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        validateStatusTimestamps(status, createdAt, disabledAt, updatedAt);

        User user = new User();
        user.id = id;
        user.name = normalizeName(name);
        user.birthDate = birthDate;
        user.email = normalizeEmail(email);
        user.cpf = normalizeCpf(cpf);
        user.rg = normalizeRg(rg);
        user.phoneNumber = normalizePhoneNumber(phoneNumber);
        user.profilePhotoUrl = normalizeProfilePhotoUrl(profilePhotoUrl);
        user.status = status;
        user.createdAt = createdAt;
        user.disabledAt = disabledAt;
        user.updatedAt = updatedAt;
        return user;
    }

    public void disable(Instant now) {
        validateMutationInstant(now);
        if (isDisabled()) {
            throw new UserAlreadyDisabledException(id);
        }

        status = UserStatus.DISABLED;
        disabledAt = now;
        updatedAt = now;
    }

    public void enable(Instant now) {
        validateMutationInstant(now);
        if (!isDisabled()) {
            throw new UserNotDisabledException(id);
        }

        status = UserStatus.ACTIVE;
        disabledAt = null;
        updatedAt = now;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isDisabled() {
        return status == UserStatus.DISABLED;
    }

    public static String normalizeName(String rawName) {
        return normalizeRequiredText(rawName, "name", MAX_NAME_LENGTH);
    }

    public static String normalizeCpf(String rawCpf) {
        String cpf = normalizeRequiredDigits(rawCpf, "cpf", 11);
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("cpf must contain exactly 11 digits");
        }
        return cpf;
    }

    public static String normalizeRg(String rawRg) {
        return normalizeOptionalDigits(rawRg, "rg", MAX_RG_LENGTH);
    }

    public static String normalizeEmail(String rawEmail) {
        String email = normalizeOptionalText(rawEmail, "email", MAX_EMAIL_LENGTH);
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    public static String normalizePhoneNumber(String rawPhoneNumber) {
        return normalizeOptionalDigits(
                rawPhoneNumber,
                "phoneNumber",
                MAX_PHONE_NUMBER_LENGTH
        );
    }

    private static String normalizeProfilePhotoUrl(String rawProfilePhotoUrl) {
        return normalizeOptionalText(
                rawProfilePhotoUrl,
                "profilePhotoUrl",
                MAX_PROFILE_PHOTO_URL_LENGTH
        );
    }

    private void validateMutationInstant(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now cannot be before createdAt");
        }
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }

    private static void validateStatusTimestamps(
            UserStatus status,
            Instant createdAt,
            Instant disabledAt,
            Instant updatedAt
    ) {
        if (status == UserStatus.ACTIVE && disabledAt != null) {
            throw new IllegalArgumentException("active user cannot have disabledAt");
        }
        if (status == UserStatus.DISABLED && disabledAt == null) {
            throw new IllegalArgumentException("disabled user must have disabledAt");
        }
        if (disabledAt != null && disabledAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("disabledAt cannot be before createdAt");
        }
        if (updatedAt != null && updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        }
    }

    private static String normalizeRequiredText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        String normalized = value.trim();
        validateLength(normalized, field, maxLength);
        return normalized;
    }

    private static String normalizeOptionalText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        validateLength(normalized, field, maxLength);
        return normalized;
    }

    private static String normalizeRequiredDigits(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        String normalized = value.replaceAll("\\D", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must contain digits");
        }
        validateLength(normalized, field, maxLength);
        return normalized;
    }

    private static String normalizeOptionalDigits(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.replaceAll("\\D", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must contain digits");
        }
        validateLength(normalized, field, maxLength);
        return normalized;
    }

    private static void validateLength(
            String value,
            String field,
            int maxLength
    ) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must have at most " + maxLength + " characters"
            );
        }
    }
}
