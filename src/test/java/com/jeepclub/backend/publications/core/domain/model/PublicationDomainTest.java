package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class PublicationDomainTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";

    @Test void validSubtypesStartAsDraftWithCommonContent() {
        List<PublicationImage> gallery = gallery(1);
        assertThat(Notice.create(7L, " Notice ", " Content ", gallery, NOW))
                .satisfies(p -> {
                    assertThat(p.getTitle()).isEqualTo("Notice");
                    assertThat(p.getContent()).isEqualTo("Content");
                    assertThat(p.getStatus()).isEqualTo(PublicationStatus.DRAFT);
                    assertThat(p.getCreatedAt()).isEqualTo(NOW);
                    assertThat(p.getPublishedAt()).isNull();
                });
        assertThat(Event.create(7L, "Event", "Content", gallery, NOW.plusSeconds(3600), NOW).getStartsAt())
                .isEqualTo(NOW.plusSeconds(3600));
        assertThat(ServicePublication.fromApprovedRequest(serviceRequest(), NOW))
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PublicationStatus.PUBLISHED));
    }

    @Test void invalidAuthorAndContentAreRejected() {
        assertThatThrownBy(() -> Notice.create(0L, "Title", "Content", gallery(1), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, " ", "Content", gallery(1), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "Title", " ", gallery(1), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void editorialTransitionsSetExplicitTimestampsAndRejectImpossibleMoves() {
        Notice notice = Notice.create(7L, "Title", "Content", gallery(1), NOW);
        assertThatThrownBy(() -> notice.archive(NOW.plusSeconds(1))).isInstanceOf(IllegalStateException.class);
        notice.publish(NOW.plusSeconds(2));
        assertThat(notice.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(notice.getPublishedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(notice.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThatThrownBy(() -> notice.publish(NOW.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
        notice.archive(NOW.plusSeconds(4));
        assertThat(notice.getStatus()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(notice.getArchivedAt()).isEqualTo(NOW.plusSeconds(4));
        assertThatThrownBy(() -> notice.archive(NOW.plusSeconds(5))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> notice.replaceImages(gallery(1), NOW.plusSeconds(5))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Notice.create(7L, "Title", "Content", gallery(1), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test void transitionCannotMoveTimeBackwards() {
        Notice notice = Notice.create(7L, "Title", "Content", gallery(1), NOW);
        assertThatThrownBy(() -> notice.publish(NOW.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void galleryAcceptsOneOrFiveAndSortsByPosition() {
        assertThat(Notice.create(7L, "Title", "Content", gallery(1), NOW).getImages()).hasSize(1);
        var reversed = new ArrayList<>(gallery(5));
        java.util.Collections.reverse(reversed);
        assertThat(Notice.create(7L, "Title", "Content", reversed, NOW).getImages())
                .extracting(PublicationImage::position).containsExactly(0, 1, 2, 3, 4);
    }

    @Test void galleryRejectsInvalidCountsPrimaryKeysAndPositions() {
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", List.of(), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", gallery(6), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", List.of(new PublicationImage(KEY, 0, false)), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", List.of(new PublicationImage(KEY, 0, true),
                new PublicationImage(key(1), 1, true)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", List.of(new PublicationImage(KEY, 0, true),
                new PublicationImage(key(1), 0, false)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notice.create(7L, "T", "C", List.of(new PublicationImage(KEY, 0, true),
                new PublicationImage(key(1), 2, false)), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicationImage("https://example.com/photo.jpg", 0, true)).isInstanceOf(RuntimeException.class);
    }

    @Test void reconstitutionPreservesConcreteTypeAndState() {
        Event event = Event.reconstitute(42L, 7L, "Trip", "Details", PublicationStatus.PUBLISHED,
                NOW, NOW.plusSeconds(1), NOW.plusSeconds(1), null, gallery(1), NOW.plusSeconds(7200));
        assertThat(event).isInstanceOf(Event.class);
        assertThat(event.getId()).isEqualTo(42L);
        assertThat(event.getStartsAt()).isEqualTo(NOW.plusSeconds(7200));
        assertThat(Notice.reconstitute(1L, 7L, "T", "C", PublicationStatus.DRAFT,
                NOW, NOW, null, null, gallery(1))).isInstanceOf(Notice.class);
        assertThat(ServicePublication.reconstitute(2L, 7L, "T", "C", PublicationStatus.PUBLISHED,
                NOW, NOW, NOW, null, gallery(1), 3L, new BigDecimal("12.00"), "123456789"))
                .isInstanceOf(ServicePublication.class);
    }

    @Test void likeAndCommentValidateAndReconstitute() {
        assertThat(PublicationLike.create(1L, 7L, NOW)).extracting(PublicationLike::memberUserId).isEqualTo(7L);
        assertThatThrownBy(() -> PublicationLike.create(0L, 7L, NOW)).isInstanceOf(IllegalArgumentException.class);
        var image = new PublicationCommentImage(KEY, 0);
        var comment = PublicationComment.create(1L, 7L, " Hello ", List.of(image), NOW);
        assertThat(comment.getContent()).isEqualTo("Hello");
        assertThat(comment.getImages()).containsExactly(image);
        assertThat(PublicationComment.reconstitute(2L, 1L, 7L, "Hello", NOW, NOW, List.of(image)).getId()).isEqualTo(2L);
        assertThatThrownBy(() -> PublicationComment.create(1L, 7L, " ", List.of(), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    private static List<PublicationImage> gallery(int count) {
        return IntStream.range(0, count).mapToObj(i -> new PublicationImage(key(i), i, i == 0)).toList();
    }

    private static ServicePublicationRequest serviceRequest() {
        return ServicePublicationRequest.reconstitute(3L, 7L, "Service", "Content", new BigDecimal("12.00"),
                "123456789", gallery(1), ServicePublicationRequestStatus.PENDING, null, null, null,
                NOW, null, NOW, 0L);
    }

    private static String key(int n) {
        return "images/2026/09/27/550e8400-e29b-41d4-a716-44665544000" + n + ".jpg";
    }
}
