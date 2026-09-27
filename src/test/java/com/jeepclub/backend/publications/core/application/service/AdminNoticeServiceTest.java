package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.application.exception.InvalidNoticeStateException;
import com.jeepclub.backend.publications.core.application.exception.NoticeNotFoundException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.Event;
import com.jeepclub.backend.publications.core.domain.model.Notice;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminNoticeServiceTest {
    private static final Instant CREATED = Instant.parse("2026-09-27T12:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(10);
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";
    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final ImageMediaService media = mock(ImageMediaService.class);
    private final AdminNoticeService service = new AdminNoticeService(repository, media, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void createUsesAuthorClockDraftAndChecksMediaFirst() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Notice created = service.create(7L, " Notice ", " Body ", gallery());
        assertThat(created.getAuthorUserId()).isEqualTo(7L);
        assertThat(created.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        var order = inOrder(media, repository);
        order.verify(media).requireExisting(KEY);
        order.verify(repository).save(any(Notice.class));
    }

    @Test void missingMediaPreventsCreateAndGalleryReplacement() {
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(KEY);
        assertThatThrownBy(() -> service.create(7L, "Notice", "Body", gallery()))
                .isInstanceOf(StorageObjectNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test void updatePreservesAbsentFieldsAndReplacesImagesWhenProvided() {
        Notice notice = draft();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(notice));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Notice edited = service.update(1L, null, " Changed ", null);
        assertThat(edited.getTitle()).isEqualTo("Original");
        assertThat(edited.getContent()).isEqualTo("Changed");
        assertThat(edited.getUpdatedAt()).isEqualTo(NOW);
        assertThat(edited.getImages()).containsExactlyElementsOf(gallery());
        Notice replaced = service.update(1L, " New ", null, List.of(new PublicationImage(NEXT, 0, true)));
        assertThat(replaced.getTitle()).isEqualTo("New");
        assertThat(replaced.getContent()).isEqualTo("Changed");
        assertThat(replaced.getImages()).containsExactly(new PublicationImage(NEXT, 0, true));
        verify(media).requireExisting(NEXT);
    }

    @Test void missingReplacementMediaDoesNotSave() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(draft()));
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(NEXT);
        assertThatThrownBy(() -> service.update(1L, "Changed", null, List.of(new PublicationImage(NEXT, 0, true))))
                .isInstanceOf(StorageObjectNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test void publishAndArchiveUseExistingLifecycleAndRejectWrongState() {
        Notice notice = draft();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(notice));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThatThrownBy(() -> service.archive(1L)).isInstanceOf(InvalidNoticeStateException.class);
        Notice published = service.publish(1L);
        assertThat(published.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.publish(1L)).isInstanceOf(InvalidNoticeStateException.class);
        Notice archived = service.archive(1L);
        assertThat(archived.getStatus()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThatThrownBy(() -> service.update(1L, "No", null, null)).isInstanceOf(InvalidNoticeStateException.class);
    }

    @Test void missingOrWrongSubtypeIsNeverManagedAsNotice() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NoticeNotFoundException.class);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(
                Event.create(7L, "Event", "Body", gallery(), NOW.plusSeconds(3600), CREATED)));
        assertThatThrownBy(() -> service.publish(1L)).isInstanceOf(NoticeNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(NoticeNotFoundException.class);
        verify(repository, never()).delete(any(), any(), any());
    }

    @Test void deleteDelegatesAuditWithAuthenticatedActorInAnyState() {
        Notice notice = draft();
        notice.publish(CREATED.plusSeconds(1));
        notice.archive(CREATED.plusSeconds(2));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(notice));
        service.delete(1L, 9L);
        verify(repository).delete(1L, 9L, NOW);
    }

    private static Notice draft() {
        return Notice.reconstitute(1L, 7L, "Original", "Body", PublicationStatus.DRAFT,
                CREATED, CREATED, null, null, gallery());
    }

    private static List<PublicationImage> gallery() { return List.of(new PublicationImage(KEY, 0, true)); }
}
