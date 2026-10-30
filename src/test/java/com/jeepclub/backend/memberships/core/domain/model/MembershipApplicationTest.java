package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipApplicationTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final String CPF = "52998224725";

    @Test
    @DisplayName("Should create application in PENDING status with trimmed and normalized fields")
    void shouldCreateApplicationSuccessfully() {
        MembershipApplication app = MembershipApplication.create(
                "  Lucas Silva  ",
                " 52998224725 ",
                " LUCAS@EXAMPLE.COM ",
                "  (11) 98888-7777  ",
                "  Gostaria de participar  ",
                NOW
        );

        assertThat(app.getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
        assertThat(app.getName()).isEqualTo("Lucas Silva");
        assertThat(app.getCpf()).isEqualTo("52998224725");
        assertThat(app.getEmail()).isEqualTo("lucas@example.com");
        assertThat(app.getPhoneNumber()).isEqualTo("(11) 98888-7777");
        assertThat(app.getMessage()).isEqualTo("Gostaria de participar");
        assertThat(app.getRequestedAt()).isEqualTo(NOW);
        assertThat(app.getUpdatedAt()).isEqualTo(NOW);
        assertThat(app.getReviewedByUserId()).isNull();
        assertThat(app.getCreatedUserId()).isNull();
        assertThat(app.getReviewedAt()).isNull();
        assertThat(app.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("Should create application with null or blank message and email")
    void shouldCreateApplicationWithNullEmailAndMessage() {
        MembershipApplication app = MembershipApplication.create(
                "Lucas Silva",
                CPF,
                "   ",
                "(11) 98888-7777",
                "   ",
                NOW
        );

        assertThat(app.getEmail()).isNull();
        assertThat(app.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should fail when mandatory fields are null or blank")
    void shouldFailWhenMandatoryFieldsAreBlank() {
        assertThatThrownBy(() -> MembershipApplication.create(null, CPF, null, "11988887777", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");

        assertThatThrownBy(() -> MembershipApplication.create("   ", CPF, null, "11988887777", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");

        assertThatThrownBy(() -> MembershipApplication.create("Lucas", null, null, "11988887777", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf must not be blank");

        assertThatThrownBy(() -> MembershipApplication.create("Lucas", CPF, null, "   ", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phoneNumber must not be blank");

        assertThatThrownBy(() -> MembershipApplication.create("Lucas", CPF, null, "11988887777", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("now must not be null");
    }

    @Test
    @DisplayName("Should approve pending application and set review fields")
    void shouldApprovePendingApplication() {
        MembershipApplication app = createPending();
        Instant reviewedAt = NOW.plusSeconds(300);

        app.approve(10L, 20L, reviewedAt);

        assertThat(app.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(app.getReviewedByUserId()).isEqualTo(10L);
        assertThat(app.getCreatedUserId()).isEqualTo(20L);
        assertThat(app.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(app.getUpdatedAt()).isEqualTo(reviewedAt);
        assertThat(app.getRejectionReason()).isNull();
        assertThat(app.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("Should reject pending application with optional reason")
    void shouldRejectPendingApplication() {
        MembershipApplication app = createPending();
        Instant rejectedAt = NOW.plusSeconds(300);

        app.reject(10L, "  Inconformidade nos dados cadastrais  ", rejectedAt);

        assertThat(app.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(app.getReviewedByUserId()).isEqualTo(10L);
        assertThat(app.getRejectionReason()).isEqualTo("Inconformidade nos dados cadastrais");
        assertThat(app.getReviewedAt()).isEqualTo(rejectedAt);
        assertThat(app.getFinishedAt()).isEqualTo(rejectedAt);
        assertThat(app.getUpdatedAt()).isEqualTo(rejectedAt);
    }

    @Test
    @DisplayName("Should complete approved application")
    void shouldCompleteApprovedApplication() {
        MembershipApplication app = createPending();
        Instant approvedAt = NOW.plusSeconds(300);
        Instant completedAt = NOW.plusSeconds(600);

        app.approve(10L, 20L, approvedAt);
        app.complete(completedAt);

        assertThat(app.getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        assertThat(app.getFinishedAt()).isEqualTo(completedAt);
        assertThat(app.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("Should throw IllegalStateException on invalid state transitions")
    void shouldRejectInvalidStateTransitions() {
        MembershipApplication pending = createPending();
        assertThatThrownBy(() -> pending.complete(NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid membership application transition: PENDING -> APPROVED");

        MembershipApplication approved = createPending();
        approved.approve(10L, 20L, NOW);
        assertThatThrownBy(() -> approved.approve(10L, 20L, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid membership application transition: APPROVED -> PENDING");
        assertThatThrownBy(() -> approved.reject(10L, "reason", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid membership application transition: APPROVED -> PENDING");

        MembershipApplication rejected = createPending();
        rejected.reject(10L, "reason", NOW);
        assertThatThrownBy(() -> rejected.approve(10L, 20L, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.complete(NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.reject(10L, "reason", NOW))
                .isInstanceOf(IllegalStateException.class);

        MembershipApplication completed = createPending();
        completed.approve(10L, 20L, NOW);
        completed.complete(NOW);
        assertThatThrownBy(() -> completed.approve(10L, 20L, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.complete(NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should correctly reconstitute application with all fields")
    void shouldReconstituteApplication() {
        MembershipApplication app = MembershipApplication.reconstitute(
                1L, "Lucas", CPF, "lucas@example.com", "11988887777", "Msg",
                MembershipApplicationStatus.APPROVED, null, 10L, 20L,
                NOW.minusSeconds(100), NOW, null, NOW, 1L
        );

        assertThat(app.getId()).isEqualTo(1L);
        assertThat(app.getName()).isEqualTo("Lucas");
        assertThat(app.getCpf()).isEqualTo(CPF);
        assertThat(app.getEmail()).isEqualTo("lucas@example.com");
        assertThat(app.getPhoneNumber()).isEqualTo("11988887777");
        assertThat(app.getMessage()).isEqualTo("Msg");
        assertThat(app.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(app.getReviewedByUserId()).isEqualTo(10L);
        assertThat(app.getCreatedUserId()).isEqualTo(20L);
        assertThat(app.getVersion()).isEqualTo(1L);
    }

    @Test
    void rejectionReasonRejectsAbuseAndNormalizesControlCharactersBeforePersistence() {
        MembershipApplication app = createPending();
        app.reject(10L, "  Original\u0000reason\r\nwith\u202econtrols  ", NOW);
        assertThat(app.getRejectionReason()).isEqualTo("Original reason  with controls");
        assertThatThrownBy(() -> createPending().reject(10L, "x".repeat(2001), NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2000");
    }

    private MembershipApplication createPending() {
        return MembershipApplication.create(
                "Lucas Silva",
                CPF,
                "lucas@example.com",
                "(11) 98888-7777",
                "Solicitação",
                NOW
        );
    }
}
