package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationChangeRequestRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicePublicationChangeApplicationTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";
    private final PublicationRepository publications = mock(PublicationRepository.class);
    private final ServicePublicationChangeRequestRepository changes = mock(ServicePublicationChangeRequestRepository.class);
    private final ImageMediaService media = mock(ImageMediaService.class);
    private final Clock clock = Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC);

    @Test void ownerCreatesPendingCompleteSnapshotWithoutChangingPublishedService() {
        var service = service();
        when(publications.findByIdForUpdate(10L)).thenReturn(Optional.of(service));
        when(changes.save(any())).thenAnswer(call -> call.getArgument(0));
        var application = new ServicePublicationChangeRequestService(publications, changes, media, clock);
        var change = application.create(10L, 7L, null, null, new BigDecimal("150"), null,
                List.of(new PublicationImage(NEXT, 0, true)));
        assertThat(change.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
        assertThat(change.getProposedTitle()).isEqualTo("Original");
        assertThat(change.getProposedContent()).isEqualTo("Body");
        assertThat(change.getProposedAmount()).isEqualByComparingTo("150.00");
        assertThat(change.getProposedContactPhone()).isEqualTo("123");
        assertThat(service.getAmount()).isEqualByComparingTo("100.00");
        verify(media).requireExisting(NEXT);
        verify(publications, never()).save(any());
    }

    @Test void ownershipPendingAndMediaFailuresPreventNewRequest() {
        when(publications.findByIdForUpdate(10L)).thenReturn(Optional.of(service()));
        var application = new ServicePublicationChangeRequestService(publications, changes, media, clock);
        assertThatThrownBy(() -> application.create(10L, 8L, "Other", null, null, null, null))
                .isInstanceOf(ServiceOperationException.class)
                .extracting("reason").isEqualTo(ServiceOperationException.Reason.SERVICE_NOT_OWNER);
        when(changes.existsPendingForService(10L)).thenReturn(true);
        assertThatThrownBy(() -> application.create(10L, 7L, "Other", null, null, null, null))
                .isInstanceOf(ServiceOperationException.class)
                .extracting("reason").isEqualTo(ServiceOperationException.Reason.CHANGE_REQUEST_ALREADY_PENDING);
        when(changes.existsPendingForService(10L)).thenReturn(false);
        doThrow(new IllegalArgumentException("missing")).when(media).requireExisting(NEXT);
        assertThatThrownBy(() -> application.create(10L, 7L, null, null, null, null,
                List.of(new PublicationImage(NEXT, 0, true)))).isInstanceOf(IllegalArgumentException.class);
        verify(changes, never()).save(any());
    }

    @Test void approvalUpdatesSameServiceBeforeClosingRequestAndRejectionLeavesItAlone() {
        var service = service();
        var change = change();
        when(changes.findByIdForUpdate(11L)).thenReturn(Optional.of(change));
        when(publications.findByIdForUpdate(10L)).thenReturn(Optional.of(service));
        when(changes.save(any())).thenAnswer(call -> call.getArgument(0));
        var application = new AdminServicePublicationChangeRequestService(changes, publications, clock);
        var result = application.approve(11L, 9L);
        assertThat(result.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.APPROVED);
        assertThat(service.getId()).isEqualTo(10L);
        assertThat(service.getTitle()).isEqualTo("Revised");
        assertThat(service.getPublishedAt()).isEqualTo(NOW);
        assertThat(service.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        var order = inOrder(changes, publications);
        order.verify(changes).findByIdForUpdate(11L);
        order.verify(publications).findByIdForUpdate(10L);
        order.verify(publications).save(service);
        order.verify(changes).save(change);

        var another = change();
        when(changes.findByIdForUpdate(12L)).thenReturn(Optional.of(another));
        application.reject(12L, 9L, " no ");
        assertThat(another.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.REJECTED);
        assertThat(another.getRejectionReason()).isEqualTo("no");
        verify(publications, times(1)).save(any());
    }

    @Test void deletedTargetKeepsChangePendingAndDoesNotSave() {
        var change = change();
        when(changes.findByIdForUpdate(11L)).thenReturn(Optional.of(change));
        when(publications.findByIdForUpdate(10L)).thenReturn(Optional.empty());
        var application = new AdminServicePublicationChangeRequestService(changes, publications, clock);
        assertThatThrownBy(() -> application.approve(11L, 9L))
                .isInstanceOf(ServiceOperationException.class)
                .extracting("reason").isEqualTo(ServiceOperationException.Reason.SERVICE_NOT_FOUND);
        assertThat(change.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
        verify(changes, never()).save(any());
    }

    private static ServicePublication service() {
        return ServicePublication.reconstitute(10L, 7L, "Original", "Body", PublicationStatus.PUBLISHED,
                NOW, NOW, NOW, null, List.of(new PublicationImage(KEY, 0, true)), 3L, new BigDecimal("100.00"), "123");
    }
    private static ServicePublicationChangeRequest change() {
        return ServicePublicationChangeRequest.reconstitute(11L, 10L, 7L, "Revised", "Revised body",
                new BigDecimal("150.00"), "456", List.of(new PublicationImage(NEXT, 0, true)),
                ServicePublicationChangeRequestStatus.PENDING, null, null, NOW, null, NOW, 0L);
    }
}
