package com.jeepclub.backend.identity.core.domain.model;

import com.jeepclub.backend.iam.identity.core.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class UserProfileTest {
    private final WorkProfile work = new WorkProfile("Mechanic", "Garage");
    private final ResidentialAddress address = new ResidentialAddress("12345-678", "Street", "S/N", null, "Center", "City", "sp");

    @Test
    void completenessRequiresBothBlocksAndEveryRequiredFieldButNotComplement() {
        assertThat(new UserProfile(1L, null, null).profileCompletionPending()).isTrue();
        assertThat(new UserProfile(1L, work, null).profileCompletionPending()).isTrue();
        assertThat(new UserProfile(1L, null, address).profileCompletionPending()).isTrue();
        assertThat(new UserProfile(1L, work, address).profileCompletionPending()).isFalse();
        assertThat(new WorkProfile(null, "Garage").isComplete()).isFalse();
        assertThat(new WorkProfile("Mechanic", null).isComplete()).isFalse();
        String[] fields = {"12345678", "Street", "1", null, "Center", "City", "SP"};
        for (int index : new int[]{0, 1, 2, 4, 5, 6}) {
            String[] partial = fields.clone();
            partial[index] = null;
            var incomplete = new ResidentialAddress(partial[0], partial[1], partial[2], partial[3], partial[4], partial[5], partial[6]);
            assertThat(new UserProfile(1L, work, incomplete).profileCompletionPending()).isTrue();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t "})
    void missingAndBlankValuesAreAbsent(String absent) {
        assertThat(new WorkProfile(absent, absent)).isEqualTo(new WorkProfile(null, null));
        assertThat(new ResidentialAddress(absent, absent, absent, absent, absent, absent, absent).isComplete()).isFalse();
    }

    @Test
    void normalizesValuesAndRejectsInvalidOrOversizedData() {
        assertThat(new WorkProfile(" Mechanic ", " Garage ")).isEqualTo(work);
        assertThat(address.postalCode()).isEqualTo("12345678");
        assertThat(address.state()).isEqualTo("SP");
        assertThatThrownBy(() -> new WorkProfile("a".repeat(151), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkProfile(null, "a".repeat(151))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResidentialAddress("abc12345678", null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResidentialAddress(null, null, null, null, null, null, "ZZ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResidentialAddress(null, null, "a".repeat(21), null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
