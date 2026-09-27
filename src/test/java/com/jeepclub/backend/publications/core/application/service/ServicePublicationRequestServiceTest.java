package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
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

class ServicePublicationRequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final Clock CLOCK = Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC);
    private final ServicePublicationRequestRepository requests = mock(ServicePublicationRequestRepository.class);
    private final PublicationRepository publications = mock(PublicationRepository.class);
    private final ImageMediaService images = mock(ImageMediaService.class);

    @Test void creationChecksMediaBeforeSavingPendingRequest() {
        when(requests.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new ServicePublicationRequestService(requests, images, CLOCK);
        var request = service.create(7L, "Service", "Body", new BigDecimal("12.00"), "123", gallery());
        assertThat(request.getStatus()).isEqualTo(ServicePublicationRequestStatus.PENDING);
        verify(images).requireExisting(KEY);
        var order = inOrder(images, requests);
        order.verify(images).requireExisting(KEY);
        order.verify(requests).save(any(ServicePublicationRequest.class));
    }

    @Test void missingMediaPreventsRequestSave() {
        doThrow(new IllegalArgumentException("missing")).when(images).requireExisting(KEY);
        var service = new ServicePublicationRequestService(requests, images, CLOCK);
        assertThatThrownBy(() -> service.create(7L, "Service", "Body", new BigDecimal("12.00"), "123", gallery()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(requests);
    }

    @Test void approvalPersistsServiceBeforeRecordingItsIdOnRequest() {
        var request = request();
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request));
        when(publications.save(any())).thenAnswer(call -> {
            var service = (ServicePublication) call.getArgument(0);
            return ServicePublication.reconstitute(11L, service.getAuthorUserId(), service.getTitle(), service.getContent(),
                    service.getStatus(), service.getCreatedAt(), service.getUpdatedAt(), service.getPublishedAt(),
                    service.getArchivedAt(), service.getImages(), service.getSourceRequestId(), service.getAmount(), service.getContactPhone());
        });
        when(requests.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new AdminServicePublicationRequestService(requests, publications, CLOCK);
        var approved = service.approve(10L, 9L);
        assertThat(approved.getStatus()).isEqualTo(ServicePublicationRequestStatus.APPROVED);
        assertThat(approved.getCreatedPublicationId()).isEqualTo(11L);
        assertThat(approved.getReviewedByUserId()).isEqualTo(9L);
        assertThat(approved.getReviewedAt()).isEqualTo(NOW.plusSeconds(1));
        var order = inOrder(requests, publications);
        order.verify(requests).findByIdForUpdate(10L);
        order.verify(publications).save(any(ServicePublication.class));
        order.verify(requests).save(argThat(r -> r.getStatus() == ServicePublicationRequestStatus.APPROVED
                && r.getCreatedPublicationId().equals(11L)));
    }

    @Test void publicationFailureLeavesRequestPendingAndUnwritten() {
        var request = request();
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request));
        when(publications.save(any())).thenThrow(new IllegalStateException("failed"));
        var service = new AdminServicePublicationRequestService(requests, publications, CLOCK);
        assertThatThrownBy(() -> service.approve(10L, 9L)).isInstanceOf(IllegalStateException.class);
        assertThat(request.getStatus()).isEqualTo(ServicePublicationRequestStatus.PENDING);
        verify(requests, never()).save(any());
    }

    @Test void rejectionDoesNotCreateService() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request()));
        when(requests.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new AdminServicePublicationRequestService(requests, publications, CLOCK);
        var rejected = service.reject(10L, 9L, "  reason  ");
        assertThat(rejected.getStatus()).isEqualTo(ServicePublicationRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("reason");
        verifyNoInteractions(publications);
    }

    private static ServicePublicationRequest request() {
        return ServicePublicationRequest.reconstitute(10L, 7L, "Service", "Body", new BigDecimal("12.00"), "123", gallery(),
                ServicePublicationRequestStatus.PENDING, null, null, null, NOW, null, NOW, 0L);
    }

    private static List<PublicationImage> gallery() { return List.of(new PublicationImage(KEY, 0, true)); }
}
