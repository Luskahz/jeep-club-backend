package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ServicePublication extends Publication {
    private final Long sourceRequestId;
    private BigDecimal amount;
    private String contactPhone;

    private ServicePublication(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                               Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                               List<PublicationImage> images, Long sourceRequestId, BigDecimal amount, String contactPhone) {
        super(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
        positive(sourceRequestId, "sourceRequestId");
        this.sourceRequestId = sourceRequestId;
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0 || amount.scale() != 2 || amount.precision() > 15) {
            throw new IllegalArgumentException("Invalid service amount.");
        }
        if (contactPhone == null || contactPhone.isBlank() || contactPhone.length() > 30) {
            throw new IllegalArgumentException("contactPhone is required and must fit storage.");
        }
        this.contactPhone = contactPhone;
        if (status == PublicationStatus.DRAFT) throw new IllegalArgumentException("An approved service cannot be a draft.");
    }

    public static ServicePublication fromApprovedRequest(ServicePublicationRequest request, Instant now) {
        Objects.requireNonNull(request, "request").requirePending();
        positive(request.getId(), "sourceRequestId");
        Objects.requireNonNull(now, "now");
        if (now.isBefore(request.getUpdatedAt())) throw new IllegalArgumentException("Publication time precedes request.");
        return new ServicePublication(null, request.getRequestedByUserId(), request.getTitle(), request.getContent(),
                PublicationStatus.PUBLISHED, now, now, now, null, request.getImages(), request.getId(),
                request.getAmount(), request.getContactPhone());
    }

    public static ServicePublication reconstitute(Long id, Long authorUserId, String title, String content,
                                                  PublicationStatus status, Instant createdAt, Instant updatedAt,
                                                  Instant publishedAt, Instant archivedAt, List<PublicationImage> images,
                                                  Long sourceRequestId, BigDecimal amount, String contactPhone) {
        positive(id, "id");
        return new ServicePublication(id, authorUserId, title, content, status, createdAt, updatedAt,
                publishedAt, archivedAt, images, sourceRequestId, amount, contactPhone);
    }

    public void applyApprovedChange(ServicePublicationChangeRequest change, Instant now) {
        Objects.requireNonNull(change, "change").requirePending();
        if (!Objects.equals(getId(), change.getServicePublicationId())
                || !Objects.equals(getAuthorUserId(), change.getRequestedByUserId())) {
            throw new IllegalArgumentException("Change request does not belong to this service.");
        }
        BigDecimal validatedAmount = ServicePublicationRequest.requireAmount(change.getProposedAmount());
        String validatedPhone = ServicePublicationRequest.required(change.getProposedContactPhone(), "contactPhone", 30);
        applyApprovedEditorialChange(change.getProposedTitle(), change.getProposedContent(), change.getProposedImages(), now);
        amount = validatedAmount;
        contactPhone = validatedPhone;
    }

    public Long getSourceRequestId() { return sourceRequestId; }
    public BigDecimal getAmount() { return amount; }
    public String getContactPhone() { return contactPhone; }
}
