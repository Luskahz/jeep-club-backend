package com.jeepclub.backend.memberships.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipApplicantBlockTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final String FORMATTED_CPF = "529.982.247-25";
    private static final String NORMALIZED_CPF = "52998224725";

    @Test
    @DisplayName("Should create active block with normalized CPF and trimmed reason")
    void shouldCreateBlockSuccessfully() {
        MembershipApplicantBlock block = MembershipApplicantBlock.create(
                FORMATTED_CPF,
                "  Tentativa de fraude cadastral  ",
                NOW,
                10L
        );

        assertThat(block.getCpf()).isEqualTo(NORMALIZED_CPF);
        assertThat(block.getReason()).isEqualTo("Tentativa de fraude cadastral");
        assertThat(block.getBlockedAt()).isEqualTo(NOW);
        assertThat(block.getBlockedByUserId()).isEqualTo(10L);
        assertThat(block.getUnblockedAt()).isNull();
        assertThat(block.getUnblockedByUserId()).isNull();
    }

    @Test
    @DisplayName("Should fail when CPF has invalid digit count or reason is blank")
    void shouldFailWhenInvalidInputs() {
        assertThatThrownBy(() -> MembershipApplicantBlock.create("123", "Reason", NOW, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf must contain 11 digits");

        assertThatThrownBy(() -> MembershipApplicantBlock.create(FORMATTED_CPF, "   ", NOW, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason must not be blank");

        assertThatThrownBy(() -> MembershipApplicantBlock.create(FORMATTED_CPF, "Reason", null, 10L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("blockedAt must not be null");

        assertThatThrownBy(() -> MembershipApplicantBlock.create(FORMATTED_CPF, "Reason", NOW, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("blockedByUserId must not be null");
    }

    @Test
    @DisplayName("Should unblock active block and set audit fields")
    void shouldUnblockActiveBlock() {
        MembershipApplicantBlock block = MembershipApplicantBlock.create(
                NORMALIZED_CPF,
                "Motivo",
                NOW,
                10L
        );

        Instant unblockedAt = NOW.plusSeconds(3600);
        block.unblock(20L, unblockedAt);

        assertThat(block.getUnblockedAt()).isEqualTo(unblockedAt);
        assertThat(block.getUnblockedByUserId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when unblocking an already inactive block")
    void shouldFailWhenUnblockingAlreadyInactiveBlock() {
        MembershipApplicantBlock block = MembershipApplicantBlock.create(
                NORMALIZED_CPF,
                "Motivo",
                NOW,
                10L
        );
        block.unblock(20L, NOW.plusSeconds(60));

        assertThatThrownBy(() -> block.unblock(20L, NOW.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Membership applicant block is already inactive");
    }

    @Test
    @DisplayName("Should reconstitute block with all fields")
    void shouldReconstituteBlock() {
        Instant unblockedAt = NOW.plusSeconds(3600);
        MembershipApplicantBlock block = MembershipApplicantBlock.reconstitute(
                1L,
                NORMALIZED_CPF,
                "Motivo",
                NOW,
                10L,
                unblockedAt,
                20L
        );

        assertThat(block.getId()).isEqualTo(1L);
        assertThat(block.getCpf()).isEqualTo(NORMALIZED_CPF);
        assertThat(block.getReason()).isEqualTo("Motivo");
        assertThat(block.getBlockedAt()).isEqualTo(NOW);
        assertThat(block.getBlockedByUserId()).isEqualTo(10L);
        assertThat(block.getUnblockedAt()).isEqualTo(unblockedAt);
        assertThat(block.getUnblockedByUserId()).isEqualTo(20L);
    }
}
