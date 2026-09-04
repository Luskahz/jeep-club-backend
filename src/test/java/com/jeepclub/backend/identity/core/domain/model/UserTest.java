package com.jeepclub.backend.identity.core.domain.model;

import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.domain.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.identity.core.domain.exception.UserNotDisabledException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createNormalizesRegistrationDataAndStartsActive() {
        User identity = User.create(
                "  Lucas Alves  ",
                LocalDate.of(2000, 5, 17),
                "  LUCAS.Alves@Example.COM ",
                "529.982.247-25",
                "12.345.678-9",
                "+55 (12) 99999-9999",
                "  https://example.com/profile.jpg  ",
                CREATED_AT
        );

        assertThat(identity.getName()).isEqualTo("Lucas Alves");
        assertThat(identity.getEmail()).isEqualTo("lucas.alves@example.com");
        assertThat(identity.getCpf()).isEqualTo("52998224725");
        assertThat(identity.getRg()).isEqualTo("123456789");
        assertThat(identity.getPhoneNumber()).isEqualTo("5512999999999");
        assertThat(identity.getProfilePhotoUrl())
                .isEqualTo("https://example.com/profile.jpg");
        assertThat(identity.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(identity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(identity.getDisabledAt()).isNull();
        assertThat(identity.getUpdatedAt()).isNull();
    }

    @Test
    void createSupportsPendingFirstAccessIdentityData() {
        User identity = User.create(
                "Pending User",
                null,
                null,
                "52998224725",
                null,
                null,
                null,
                CREATED_AT
        );

        assertThat(identity.getBirthDate()).isNull();
        assertThat(identity.getEmail()).isNull();
        assertThat(identity.getRg()).isNull();
        assertThat(identity.getPhoneNumber()).isNull();
    }

    @Test
    void disableAndEnableMaintainAdministrativeTimestamps() {
        User identity = persistedIdentity(UserStatus.ACTIVE, null, null);
        Instant disabledAt = CREATED_AT.plusSeconds(60);
        Instant enabledAt = CREATED_AT.plusSeconds(120);

        identity.disable(disabledAt);

        assertThat(identity.isDisabled()).isTrue();
        assertThat(identity.getDisabledAt()).isEqualTo(disabledAt);
        assertThat(identity.getUpdatedAt()).isEqualTo(disabledAt);

        identity.enable(enabledAt);

        assertThat(identity.isActive()).isTrue();
        assertThat(identity.getDisabledAt()).isNull();
        assertThat(identity.getUpdatedAt()).isEqualTo(enabledAt);
    }

    @Test
    void repeatedAdministrativeTransitionIsRejected() {
        User active = persistedIdentity(UserStatus.ACTIVE, null, null);
        User disabled = persistedIdentity(
                UserStatus.DISABLED,
                CREATED_AT.plusSeconds(10),
                CREATED_AT.plusSeconds(10)
        );

        assertThatThrownBy(() -> active.enable(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(UserNotDisabledException.class);
        assertThatThrownBy(() -> disabled.disable(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(UserAlreadyDisabledException.class);
    }

    @Test
    void invalidNormalizedDocumentsAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.normalizeCpf("123"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.normalizeRg("RG"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.normalizePhoneNumber("phone"));
    }

    @Test
    void reconstitutionRejectsInconsistentStatusTimestamps() {
        assertThatIllegalArgumentException().isThrownBy(() -> User.reconstitute(
                1L,
                "Lucas Alves",
                null,
                null,
                "52998224725",
                null,
                null,
                null,
                UserStatus.DISABLED,
                CREATED_AT,
                null,
                null
        ));
    }

    private User persistedIdentity(
            UserStatus status,
            Instant disabledAt,
            Instant updatedAt
    ) {
        return User.reconstitute(
                1L,
                "Lucas Alves",
                null,
                "lucas@example.com",
                "52998224725",
                "123456789",
                "12999999999",
                null,
                status,
                CREATED_AT,
                disabledAt,
                updatedAt
        );
    }
}
