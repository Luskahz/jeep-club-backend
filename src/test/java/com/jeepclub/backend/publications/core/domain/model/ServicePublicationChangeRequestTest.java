package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ServicePublicationChangeRequestTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";

    @Test void createsCompletePendingSnapshotAndReconstitutesIt() {
        var change = create();
        assertThat(change.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
        assertThat(change.getRequestedAt()).isEqualTo(NOW);
        assertThat(change.getUpdatedAt()).isEqualTo(NOW);
        assertThat(change.getProposedAmount()).isEqualByComparingTo("150.00");
        assertThat(change.getProposedImages()).containsExactly(new PublicationImage(NEXT, 0, true));
        var restored = ServicePublicationChangeRequest.reconstitute(11L, 10L, 7L, change.getProposedTitle(),
                change.getProposedContent(), change.getProposedAmount(), change.getProposedContactPhone(),
                change.getProposedImages(), change.getStatus(), null, null, NOW, null, NOW, 0L);
        assertThat(restored.getId()).isEqualTo(11L);
        assertThat(restored.getProposedTitle()).isEqualTo("Revised");
    }

    @Test void approveAndRejectAreTerminalAndTimestamped() {
        var approved = create();
        approved.approve(9L, NOW.plusSeconds(1));
        assertThat(approved.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.APPROVED);
        assertThat(approved.getReviewedByUserId()).isEqualTo(9L);
        assertThat(approved.getReviewedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(approved.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> approved.reject(9L, null, NOW.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
        var rejected = create();
        rejected.reject(9L, "  no  ", NOW.plusSeconds(2));
        assertThat(rejected.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("no");
        assertThatThrownBy(() -> rejected.approve(9L, NOW.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
    }

    @Test void proposedFieldsGalleryAndReviewTimeAreValidated() {
        assertThatThrownBy(() -> ServicePublicationChangeRequest.create(10L, 0L, "Title", "Body",
                amount(), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationChangeRequest.create(10L, 7L, " ", "Body",
                amount(), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationChangeRequest.create(10L, 7L, "Title", "Body",
                BigDecimal.ZERO, "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationChangeRequest.create(10L, 7L, "Title", "Body",
                amount(), " ", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationChangeRequest.create(10L, 7L, "Title", "Body",
                amount(), "123", List.of(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create().approve(9L, NOW.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void applyingApprovedSnapshotKeepsServiceIdentityPublicationTimeAndSocialTarget() {
        var service = ServicePublication.reconstitute(10L, 7L, "Original", "Body", PublicationStatus.PUBLISHED,
                NOW, NOW, NOW, null, gallery(), 3L, amount(), "123");
        service.applyApprovedChange(create(), NOW.plusSeconds(2));
        assertThat(service.getId()).isEqualTo(10L);
        assertThat(service.getSourceRequestId()).isEqualTo(3L);
        assertThat(service.getAuthorUserId()).isEqualTo(7L);
        assertThat(service.getTitle()).isEqualTo("Revised");
        assertThat(service.getContent()).isEqualTo("Revised body");
        assertThat(service.getAmount()).isEqualByComparingTo("150.00");
        assertThat(service.getContactPhone()).isEqualTo("456");
        assertThat(service.getImages()).containsExactly(new PublicationImage(NEXT, 0, true));
        assertThat(service.getCreatedAt()).isEqualTo(NOW);
        assertThat(service.getPublishedAt()).isEqualTo(NOW);
        assertThat(service.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(service.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        service.archive(NOW.plusSeconds(3));
        assertThatThrownBy(() -> service.applyApprovedChange(create(), NOW.plusSeconds(4)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ServicePublicationChangeRequest create() {
        return ServicePublicationChangeRequest.create(10L, 7L, " Revised ", " Revised body ",
                new BigDecimal("150"), " 456 ", List.of(new PublicationImage(NEXT, 0, true)), NOW);
    }
    private static BigDecimal amount() { return new BigDecimal("100.00"); }
    private static List<PublicationImage> gallery() { return List.of(new PublicationImage(KEY, 0, true)); }
}
