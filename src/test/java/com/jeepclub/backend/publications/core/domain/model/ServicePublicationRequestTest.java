package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServicePublicationRequestTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String OTHER = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";

    @Test void creationIsPendingWithNormalizedDataAndImages() {
        var request = create();
        assertThat(request.getStatus()).isEqualTo(ServicePublicationRequestStatus.PENDING);
        assertThat(request.getRequestedAt()).isEqualTo(NOW);
        assertThat(request.getUpdatedAt()).isEqualTo(NOW);
        assertThat(request.getTitle()).isEqualTo("Service");
        assertThat(request.getAmount()).isEqualByComparingTo("12.00");
        assertThat(request.getContactPhone()).isEqualTo("123456789");
        assertThat(request.getImages()).extracting(PublicationImage::position).containsExactly(0, 1);
        assertThat(request.getReviewedAt()).isNull();
        var fiveImages = java.util.stream.IntStream.range(0, 5).mapToObj(i -> new PublicationImage(
                "images/2026/09/27/550e8400-e29b-41d4-a716-44665544000" + i + ".jpg", i, i == 0)).toList();
        assertThat(ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123", fiveImages, NOW).getImages()).hasSize(5);
    }

    @Test void requiredFieldsAndGalleryAreValidated() {
        assertThatThrownBy(() -> ServicePublicationRequest.create(0L, "Service", "Body", amount(), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, " ", "Body", amount(), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", " ", amount(), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", BigDecimal.ZERO, "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", new BigDecimal("1.001"), "123", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), " ", gallery(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123", List.of(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123",
                java.util.stream.IntStream.range(0, 6).mapToObj(i -> new PublicationImage(
                        "images/2026/09/27/550e8400-e29b-41d4-a716-44665544000" + i + ".jpg", i, i == 0)).toList(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123",
                List.of(new PublicationImage(KEY, 0, false)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123",
                List.of(new PublicationImage(KEY, 0, true), new PublicationImage(OTHER, 1, true)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123",
                List.of(new PublicationImage(KEY, 0, true), new PublicationImage(OTHER, 2, false)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServicePublicationRequest.create(7L, "Service", "Body", amount(), "123",
                List.of(new PublicationImage(KEY, 0, true), new PublicationImage(KEY, 1, false)), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void approvalCreatesPublishedServiceAndClosesRequest() {
        var request = restored();
        var service = ServicePublication.fromApprovedRequest(request, NOW.plusSeconds(2));
        assertThat(service.getSourceRequestId()).isEqualTo(10L);
        assertThat(service.getAuthorUserId()).isEqualTo(7L);
        assertThat(service.getTitle()).isEqualTo(request.getTitle());
        assertThat(service.getContent()).isEqualTo(request.getContent());
        assertThat(service.getAmount()).isEqualTo(request.getAmount());
        assertThat(service.getContactPhone()).isEqualTo(request.getContactPhone());
        assertThat(service.getImages()).containsExactlyElementsOf(request.getImages());
        assertThat(service.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(service.getCreatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(service.getPublishedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(service.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(service.getArchivedAt()).isNull();
        request.approve(9L, 11L, NOW.plusSeconds(2));
        assertThat(request.getStatus()).isEqualTo(ServicePublicationRequestStatus.APPROVED);
        assertThat(request.getReviewedByUserId()).isEqualTo(9L);
        assertThat(request.getCreatedPublicationId()).isEqualTo(11L);
        assertThat(request.getReviewedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(request.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThatThrownBy(() -> request.approve(9L, 12L, NOW.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request.reject(9L, null, NOW.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ServicePublication.fromApprovedRequest(request, NOW.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectionNeverCreatesPublicationAndIsTerminal() {
        var request = restored();
        request.reject(9L, "  unsuitable  ", NOW.plusSeconds(1));
        assertThat(request.getStatus()).isEqualTo(ServicePublicationRequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("unsuitable");
        assertThat(request.getCreatedPublicationId()).isNull();
        assertThat(request.getReviewedByUserId()).isEqualTo(9L);
        assertThat(request.getReviewedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> request.approve(9L, 11L, NOW.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request.reject(9L, null, NOW.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ServicePublication.fromApprovedRequest(request, NOW.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
    }

    private static ServicePublicationRequest create() {
        return ServicePublicationRequest.create(7L, " Service ", " Body ", amount(), " 123456789 ", gallery(), NOW);
    }

    private static ServicePublicationRequest restored() {
        return ServicePublicationRequest.reconstitute(10L, 7L, "Service", "Body", amount(), "123456789", gallery(),
                ServicePublicationRequestStatus.PENDING, null, null, null, NOW, null, NOW, 0L);
    }

    private static BigDecimal amount() { return new BigDecimal("12.00"); }
    private static List<PublicationImage> gallery() {
        return List.of(new PublicationImage(OTHER, 1, false), new PublicationImage(KEY, 0, true));
    }
}
