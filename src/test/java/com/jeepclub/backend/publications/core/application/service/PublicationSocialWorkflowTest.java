package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicationSocialWorkflowTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";

    @Test void likeIsIdempotentAndUnlikeUsesAuthenticatedMember() {
        var publications = mock(PublicationRepository.class);
        var likes = mock(PublicationLikeRepository.class);
        var notice = publishedNotice();
        when(publications.findByIdForUpdate(1L)).thenReturn(Optional.of(notice));
        when(likes.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new PublicationLikeService(publications, likes, CLOCK);
        service.like(1L, 9L);
        var existing = new PublicationLike(7L, 1L, 9L, NOW);
        when(likes.findByPublicationAndMember(1L, 9L)).thenReturn(Optional.of(existing));
        assertThat(service.like(1L, 9L)).isEqualTo(existing);
        verify(likes, times(1)).save(any());
        service.unlike(1L, 9L);
        verify(likes).delete(existing);
    }

    @Test void draftAndArchiveAreNotAccessibleForSocialWrites() {
        var publications = mock(PublicationRepository.class);
        var likes = mock(PublicationLikeRepository.class);
        var draft = Notice.create(7L, "Notice", "Body", gallery(), NOW.minusSeconds(2));
        when(publications.findByIdForUpdate(1L)).thenReturn(Optional.of(draft));
        var service = new PublicationLikeService(publications, likes, CLOCK);
        assertThatThrownBy(() -> service.like(1L, 9L)).isInstanceOf(PublicationNotFoundException.class);
        draft.publish(NOW.minusSeconds(1));
        draft.archive(NOW);
        assertThatThrownBy(() -> service.like(1L, 9L)).isInstanceOf(PublicationNotFoundException.class);
        verifyNoInteractions(likes);
    }

    @Test void imageOnlyCommentIsAllowedAndEmptyCommentIsRejected() {
        var publications = mock(PublicationRepository.class);
        var comments = mock(PublicationCommentRepository.class);
        var images = mock(ImageMediaService.class);
        when(publications.findByIdForUpdate(1L)).thenReturn(Optional.of(publishedNotice()));
        when(comments.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new PublicationCommentService(publications, comments, images, CLOCK);
        assertThat(service.create(1L, 9L, null, List.of(new PublicationCommentImage(KEY, 0))).getContent()).isEmpty();
        verify(images).requireExisting(KEY);
        assertThatThrownBy(() -> service.create(1L, 9L, " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(comments, times(1)).save(any());
    }

    @Test void finishedPublishedEventStillAcceptsComment() {
        var publications = mock(PublicationRepository.class);
        var comments = mock(PublicationCommentRepository.class);
        var images = mock(ImageMediaService.class);
        var event = Event.reconstitute(1L, 7L, "Trail", "Body",
                com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
                NOW.minusSeconds(100), NOW.minusSeconds(90), NOW.minusSeconds(90), null,
                gallery(), NOW.minusSeconds(80), NOW.minusSeconds(10),
                com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN);
        when(publications.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(comments.save(any())).thenAnswer(call -> call.getArgument(0));
        var comment = new PublicationCommentService(publications, comments, images, CLOCK)
                .create(1L, 9L, "Still discussing", List.of());
        assertThat(event.effectiveStatus(NOW).name()).isEqualTo("FINISHED");
        assertThat(comment.getContent()).isEqualTo("Still discussing");
    }

    @Test void batchSummaryDoesNotLoadIndividualInteractions() {
        var likes = mock(PublicationLikeRepository.class);
        var comments = mock(PublicationCommentRepository.class);
        when(likes.counts(List.of(1L, 2L))).thenReturn(Map.of(1L, 2L));
        when(comments.counts(List.of(1L, 2L))).thenReturn(Map.of(2L, 3L));
        when(likes.likedByMember(List.of(1L, 2L), 9L)).thenReturn(Set.of(1L));
        var result = new PublicationSocialSummaryQuery(likes, comments).get(List.of(1L, 2L), 9L);
        assertThat(result.get(1L)).isEqualTo(new PublicationSocialSummaryQuery.Summary(2, 0, true));
        assertThat(result.get(2L)).isEqualTo(new PublicationSocialSummaryQuery.Summary(0, 3, false));
        verify(likes).counts(List.of(1L, 2L));
        verify(likes).likedByMember(List.of(1L, 2L), 9L);
        verify(comments).counts(List.of(1L, 2L));
        verifyNoMoreInteractions(likes, comments);
    }

    private static Notice publishedNotice() {
        var notice = Notice.create(7L, "Notice", "Body", gallery(), NOW.minusSeconds(1));
        notice.publish(NOW);
        return notice;
    }
    private static List<PublicationImage> gallery() { return List.of(new PublicationImage(KEY, 0, true)); }
}
