package com.jeepclub.backend.identity.core.domain.model;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class AdminUserMutationTest {
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private User user() {
        return User.reconstitute(42L, "Original", null, "old@example.com", "52998224725", "123", "11999999999", null,
                UserStatus.INACTIVE, NOW.minusSeconds(60), NOW.minusSeconds(30), NOW.minusSeconds(30));
    }
    @Test void changesAllDataAtomicallyWithoutChangingLifecycleOrIdentity() {
        User user = user();
        user.updateAdministratively(" New ", null, " NEW@example.com ", "111.444.777-35", "22-3", "(11) 98888-7777", null, NOW);
        assertThat(user.getCpf()).isEqualTo("11144477735");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getRg()).isEqualTo("223");
        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getDisabledAt()).isEqualTo(NOW.minusSeconds(30));
        assertThat(user.getCreatedAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }
    @ParameterizedTest @ValueSource(strings={"11111111111","52998224724","5299822472","x52998224725","","529 982 247 25"})
    void invalidCpfNeverPartiallyMutates(String cpf) {
        User user = user();
        assertThatThrownBy(() -> user.updateAdministratively("New", null, "new@example.com", cpf, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getName()).isEqualTo("Original");
        assertThat(user.getEmail()).isEqualTo("old@example.com");
        assertThat(user.getUpdatedAt()).isEqualTo(NOW.minusSeconds(30));
    }
    @Test void invalidPhotoDoesNotMutateEvenPreviouslyValidatedContactFields() {
        User user = user();
        assertThatThrownBy(() -> user.updateAdministratively("New", null, "new@example.com", "11144477735", null, null, "https://host/photo.png", NOW))
                .isInstanceOf(com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException.class);
        assertThat(user.getCpf()).isEqualTo("52998224725");
        assertThat(user.getEmail()).isEqualTo("old@example.com");
        assertThat(user.getName()).isEqualTo("Original");
    }
    @Test void unrelatedEditsPreserveLegacyCpfWithoutNewChecksumRequirements() {
        User user = User.reconstitute(42L, "Legacy", null, null, "12345678901", null, null, null,
                UserStatus.ACTIVE, NOW.minusSeconds(60), null, null);
        user.updateAdministratively("Corrected", null, null, user.getCpf(), null, null, null, NOW);
        assertThat(user.getCpf()).isEqualTo("12345678901");
    }
}
