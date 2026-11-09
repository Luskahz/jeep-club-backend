package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.api.http.dto.PublicationFeedDTO;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicationFeedServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";

    @Test void feedUsesDeterministicSortAndOneBatchSummary() {
        var publications = mock(PublicationRepository.class);
        var social = mock(PublicationSocialSummaryQuery.class);
        var notice = Notice.reconstitute(1L, 7L, "Notice", "Body", com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
                NOW.minusSeconds(1), NOW, NOW, null, gallery());
        when(publications.findPublished(eq("NOTICE"), isNull(), isNull(), any())).thenAnswer(call ->
                new PageImpl<Publication>(List.of(notice), call.getArgument(3), 1));
        when(social.get(List.of(1L), 9L)).thenReturn(Map.of(1L, new PublicationSocialSummaryQuery.Summary(3, 2, true)));
        var result = new PublicationFeedService(publications, social, mock(com.jeepclub.backend.publications.core.repository.EventParticipantRepository.class)).feed("NOTICE", null, null, PageRequest.of(0, 20), 9L);
        assertThat(result.getContent()).hasSize(1);
        var dto = PublicationFeedDTO.Item.from(result.getContent().get(0), NOW);
        assertThat(dto.type()).isEqualTo("NOTICE");
        assertThat(dto.likeCount()).isEqualTo(3);
        assertThat(dto.commentCount()).isEqualTo(2);
        assertThat(dto.likedByMe()).isTrue();
        var sort = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(publications).findPublished(eq("NOTICE"), isNull(), isNull(), sort.capture());
        assertThat(sort.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        verify(social).get(List.of(1L), 9L);
    }

    @Test void feedBatchesRegistrationCountsForEvents() {
        var publications = mock(PublicationRepository.class);
        var social = mock(PublicationSocialSummaryQuery.class);
        var counts = mock(com.jeepclub.backend.publications.core.repository.EventParticipantRepository.class);
        var event = Event.reconstitute(2L, 7L, "Trail", "Body",
                com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
                NOW.minusSeconds(100), NOW.minusSeconds(90), NOW.minusSeconds(90), null, gallery(),
                NOW.plusSeconds(80), null, com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN);
        var other = Event.reconstitute(3L, 7L, "Trail 2", "Body",
                com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
                NOW.minusSeconds(100), NOW.minusSeconds(90), NOW.minusSeconds(90), null, gallery(),
                NOW.plusSeconds(80), null, com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN);
        when(publications.findPublished(isNull(), isNull(), isNull(), any())).thenAnswer(call ->
                new PageImpl<Publication>(List.of(event, other), call.getArgument(3), 2));
        when(social.get(List.of(2L, 3L), 9L)).thenReturn(Map.of(2L, new PublicationSocialSummaryQuery.Summary(0, 0, false),
                3L, new PublicationSocialSummaryQuery.Summary(0, 0, false)));
        when(counts.confirmedCounts(List.of(2L, 3L))).thenReturn(Map.of(2L, 4L));
        var page = new PublicationFeedService(publications, social, counts).feed(null, null, null, PageRequest.of(0, 20), 9L);
        assertThat(((PublicationFeedDTO.EventDetails) PublicationFeedDTO.Item.from(page.getContent().get(0), NOW).details()).registrationCount()).isEqualTo(4);
        assertThat(((PublicationFeedDTO.EventDetails) PublicationFeedDTO.Item.from(page.getContent().get(1), NOW).details()).registrationCount()).isZero();
        verify(counts, times(1)).confirmedCounts(List.of(2L, 3L));
        verifyNoMoreInteractions(counts);
    }

    @Test void draftDetailIsHidden() {
        var publications = mock(PublicationRepository.class);
        var social = mock(PublicationSocialSummaryQuery.class);
        when(publications.findById(1L)).thenReturn(Optional.of(Notice.create(7L, "Draft", "Body", gallery(), NOW)));
        assertThatThrownBy(() -> new PublicationFeedService(publications, social, mock(com.jeepclub.backend.publications.core.repository.EventParticipantRepository.class)).detail(1L, 9L))
                .isInstanceOf(PublicationNotFoundException.class);
        verifyNoInteractions(social);
    }

    @Test void finishedPublishedEventRemainsVisibleAndCarriesSpecificStatus() {
        var publications = mock(PublicationRepository.class);
        var social = mock(PublicationSocialSummaryQuery.class);
        var event = Event.reconstitute(2L, 7L, "Trail", "Body",
                com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
                NOW.minusSeconds(100), NOW.minusSeconds(90), NOW.minusSeconds(90), null, gallery(),
                NOW.minusSeconds(80), NOW.minusSeconds(10),
                com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN);
        when(publications.findById(2L)).thenReturn(Optional.of(event));
        when(social.get(List.of(2L), 9L)).thenReturn(Map.of(2L, new PublicationSocialSummaryQuery.Summary(0, 0, false)));
        var participants = mock(com.jeepclub.backend.publications.core.repository.EventParticipantRepository.class);
        when(participants.confirmedCounts(List.of(2L))).thenReturn(Map.of(2L, 3L));
        var dto = PublicationFeedDTO.Detail.from(new PublicationFeedService(publications, social, participants).detail(2L, 9L), NOW);
        verify(participants).confirmedCounts(List.of(2L));
        assertThat(dto.type()).isEqualTo("EVENT");
        assertThat(dto.details()).isEqualTo(new PublicationFeedDTO.EventDetails(event.getStartsAt(), event.getEndsAt(), "FINISHED", 3L));
    }

    private static List<PublicationImage> gallery() { return List.of(new PublicationImage(KEY, 0, true)); }
}
