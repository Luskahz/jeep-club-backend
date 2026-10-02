package com.jeepclub.backend.health.core.domain;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class MedicalProfileInvariantTest {
    private static final Instant CREATED = Instant.parse("2026-09-20T12:00:00Z");
    private static final Instant UPDATED = CREATED.plusSeconds(60);

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidOwnerOnCreationAndReconstitution(Long owner) {
        assertThatThrownBy(() -> MedicalProfile.create(MedicalProfileOwnerType.USER, owner, null,
                null, null, null, null, null, null, null, null, null, null, CREATED))
                .isInstanceOf(InvalidMedicalProfileException.class);
        assertThatThrownBy(() -> restore(1L, MedicalProfileOwnerType.DEPENDENT, owner, CREATED, UPDATED))
                .isInstanceOf(InvalidMedicalProfileException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonpositivePersistedId(Long id) {
        assertThatThrownBy(() -> restore(id, MedicalProfileOwnerType.USER, 1L, CREATED, UPDATED))
                .isInstanceOf(InvalidMedicalProfileException.class);
    }

    @Test
    void reconstitutionRequiresOwnerTypeAndOrderedNonnullDates() {
        assertThatThrownBy(() -> restore(1L, null, 1L, CREATED, UPDATED)).isInstanceOf(InvalidMedicalProfileException.class);
        assertThatThrownBy(() -> restore(1L, MedicalProfileOwnerType.USER, 1L, null, UPDATED)).isInstanceOf(InvalidMedicalProfileException.class);
        assertThatThrownBy(() -> restore(1L, MedicalProfileOwnerType.USER, 1L, CREATED, null)).isInstanceOf(InvalidMedicalProfileException.class);
        assertThatThrownBy(() -> restore(1L, MedicalProfileOwnerType.USER, 1L, CREATED, CREATED.minusNanos(1))).isInstanceOf(InvalidMedicalProfileException.class);
        assertThat(restore(1L, MedicalProfileOwnerType.USER, 1L, CREATED, CREATED).getUpdatedAt()).isEqualTo(CREATED);
    }

    @Test
    void creationRequiresOperationTime() {
        assertThatThrownBy(() -> MedicalProfile.create(MedicalProfileOwnerType.USER, 1L, null,
                null, null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidMedicalProfileException.class);
    }

    @Test
    void rejectedUpdatesPreserveDataAndTimestampsIncludingLateFieldValidation() {
        var profile = restore(1L, MedicalProfileOwnerType.USER, 2L, CREATED, UPDATED);
        for (Instant invalid : new Instant[]{null, CREATED.minusNanos(1), UPDATED.minusNanos(1)}) {
            assertThatThrownBy(() -> update(profile, invalid, "SYNTHETIC_CHANGED", null))
                    .isInstanceOf(InvalidMedicalProfileException.class);
            assertThat(profile.getAllergies()).isEqualTo("SYNTHETIC_ORIGINAL");
            assertThat(profile.getUpdatedAt()).isEqualTo(UPDATED);
        }
        assertThatThrownBy(() -> update(profile, UPDATED.plusSeconds(1), "SYNTHETIC_CHANGED", "x".repeat(2001)))
                .isInstanceOf(InvalidMedicalProfileException.class);
        assertThat(profile.getAllergies()).isEqualTo("SYNTHETIC_ORIGINAL");
        assertThat(profile.getUpdatedAt()).isEqualTo(UPDATED);
    }

    @Test
    void equalTimestampIsAllowedAndReplacementClearsDataWithoutChangingIdentity() {
        var profile = restore(1L, MedicalProfileOwnerType.DEPENDENT, 2L, CREATED, UPDATED);
        update(profile, UPDATED, null, null);
        assertThat(profile.getId()).isEqualTo(1L);
        assertThat(profile.getOwnerType()).isEqualTo(MedicalProfileOwnerType.DEPENDENT);
        assertThat(profile.getOwnerId()).isEqualTo(2L);
        assertThat(profile.getCreatedAt()).isEqualTo(CREATED);
        assertThat(profile.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(profile.getAllergies()).isNull();
        assertThat(profile.getBloodType()).isEqualTo(BloodType.UNKNOWN);
    }

    private MedicalProfile restore(Long id, MedicalProfileOwnerType type, Long owner, Instant created, Instant updated) {
        return MedicalProfile.reconstitute(id, type, owner, BloodType.A_POSITIVE, "SYNTHETIC_ORIGINAL",
                null, null, null, null, null, null, null, null, null, created, updated);
    }

    private void update(MedicalProfile profile, Instant at, String text, String observations) {
        profile.update(null, text, null, null, null, null, null, null, null, null, observations, at);
    }
}
