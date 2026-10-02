package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.PublicationCommentRepository;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final ImageMediaService images = mock(ImageMediaService.class);
    private final PublicationService service = new PublicationService(repository, images, CLOCK);

    @Test void createChecksGlobalMediaBeforePersisting() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Notice created = service.createNotice(7L, "Notice", "Body", gallery());
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        verify(images).requireExisting(KEY);
        verify(repository).save(any(Notice.class));
    }

    @Test void missingMediaPreventsPersistence() {
        doThrow(new IllegalArgumentException("missing")).when(images).requireExisting(KEY);
        assertThatThrownBy(() -> service.createNotice(7L, "Notice", "Body", gallery()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test void lifecycleUsesClockAndLockedRead() {
        Notice draft = Notice.create(7L, "Notice", "Body", gallery(), NOW.minusSeconds(1));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(draft));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Publication published = service.publish(1L);
        assertThat(published.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isEqualTo(NOW);
        verify(repository).findByIdForUpdate(1L);
        service.delete(1L, 9L);
        verify(repository).delete(1L, 9L, NOW);
        verifyNoInteractions(images);
    }

    @Test void commentAssociationChecksGlobalMedia() {
        PublicationCommentRepository comments = mock(PublicationCommentRepository.class);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(publishedNotice()));
        when(comments.save(any())).thenAnswer(call -> call.getArgument(0));
        PublicationCommentService commentService = new PublicationCommentService(repository, comments, images, CLOCK);
        PublicationComment result = commentService.create(1L, 9L, "Hello", List.of(new PublicationCommentImage(KEY, 0)));
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        verify(images).requireExisting(KEY);
        verify(comments).save(any(PublicationComment.class));
    }

    @Test void missingCommentMediaPreventsPersistence() {
        PublicationCommentRepository comments = mock(PublicationCommentRepository.class);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(publishedNotice()));
        doThrow(new IllegalArgumentException("missing")).when(images).requireExisting(KEY);
        var commentService = new PublicationCommentService(repository, comments, images, CLOCK);
        assertThatThrownBy(() -> commentService.create(1L, 9L, "Hello", List.of(new PublicationCommentImage(KEY, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(comments);
    }

    private static List<PublicationImage> gallery() {
        return List.of(new PublicationImage(KEY, 0, true));
    }

    private static Notice publishedNotice() {
        var notice = Notice.create(7L, "Notice", "Body", gallery(), NOW.minusSeconds(1));
        notice.publish(NOW);
        return notice;
    }
}
