package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A complete proposed public snapshot; never a Publication. */
public final class ServicePublicationChangeRequest {
    private final Long id;
    private final Long servicePublicationId;
    private final Long requestedByUserId;
    private final String proposedTitle;
    private final String proposedContent;
    private final BigDecimal proposedAmount;
    private final String proposedContactPhone;
    private final List<PublicationImage> proposedImages;
    private ServicePublicationChangeRequestStatus status;
    private String rejectionReason;
    private Long reviewedByUserId;
    private final Instant requestedAt;
    private Instant reviewedAt;
    private Instant updatedAt;
    private final Long version;

    private ServicePublicationChangeRequest(Long id, Long servicePublicationId, Long requestedByUserId,
            String proposedTitle, String proposedContent, BigDecimal proposedAmount, String proposedContactPhone,
            List<PublicationImage> proposedImages, ServicePublicationChangeRequestStatus status,
            String rejectionReason, Long reviewedByUserId, Instant requestedAt, Instant reviewedAt,
            Instant updatedAt, Long version) {
        if (id != null) positive(id, "id");
        positive(servicePublicationId, "servicePublicationId");
        positive(requestedByUserId, "requestedByUserId");
        if (version != null && version < 0) throw new IllegalArgumentException("version must be non-negative.");
        this.id = id;
        this.servicePublicationId = servicePublicationId;
        this.requestedByUserId = requestedByUserId;
        this.proposedTitle = ServicePublicationRequest.required(proposedTitle, "proposedTitle", 200);
        this.proposedContent = ServicePublicationRequest.required(proposedContent, "proposedContent", Integer.MAX_VALUE);
        this.proposedAmount = ServicePublicationRequest.requireAmount(proposedAmount);
        this.proposedContactPhone = ServicePublicationRequest.required(proposedContactPhone, "proposedContactPhone", 30);
        this.proposedImages = PublicationGallery.of(proposedImages).images();
        this.status = Objects.requireNonNull(status, "status");
        this.rejectionReason = ServicePublicationRequest.normalizeNullable(rejectionReason);
        this.reviewedByUserId = reviewedByUserId;
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.reviewedAt = reviewedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
        if (updatedAt.isBefore(requestedAt) || (reviewedAt != null &&
                (reviewedAt.isBefore(requestedAt) || updatedAt.isBefore(reviewedAt)))) {
            throw new IllegalArgumentException("Inconsistent change request timestamps.");
        }
        if (status == ServicePublicationChangeRequestStatus.PENDING) {
            if (reviewedByUserId != null || reviewedAt != null || this.rejectionReason != null)
                throw new IllegalArgumentException("Pending change request cannot contain review results.");
        } else {
            positive(reviewedByUserId, "reviewedByUserId");
            Objects.requireNonNull(reviewedAt, "reviewedAt");
            if (status == ServicePublicationChangeRequestStatus.APPROVED && this.rejectionReason != null)
                throw new IllegalArgumentException("Approved change request cannot have a rejection reason.");
        }
    }

    public static ServicePublicationChangeRequest create(Long servicePublicationId, Long requestedByUserId,
            String title, String content, BigDecimal amount, String contactPhone, List<PublicationImage> images,
            Instant now) {
        Objects.requireNonNull(now, "now");
        return new ServicePublicationChangeRequest(null, servicePublicationId, requestedByUserId, title, content,
                amount, contactPhone, images, ServicePublicationChangeRequestStatus.PENDING,
                null, null, now, null, now, null);
    }

    public static ServicePublicationChangeRequest reconstitute(Long id, Long servicePublicationId,
            Long requestedByUserId, String title, String content, BigDecimal amount, String contactPhone,
            List<PublicationImage> images, ServicePublicationChangeRequestStatus status, String rejectionReason,
            Long reviewedByUserId, Instant requestedAt, Instant reviewedAt, Instant updatedAt, Long version) {
        positive(id, "id");
        Objects.requireNonNull(version, "version");
        return new ServicePublicationChangeRequest(id, servicePublicationId, requestedByUserId, title, content,
                amount, contactPhone, images, status, rejectionReason, reviewedByUserId,
                requestedAt, reviewedAt, updatedAt, version);
    }

    public void requirePending() {
        if (status != ServicePublicationChangeRequestStatus.PENDING)
            throw new IllegalStateException("Change request has already been reviewed: " + status);
    }

    public void approve(Long reviewer, Instant now) {
        review(reviewer, now);
        status = ServicePublicationChangeRequestStatus.APPROVED;
        rejectionReason = null;
    }

    public void reject(Long reviewer, String reason, Instant now) {
        review(reviewer, now);
        status = ServicePublicationChangeRequestStatus.REJECTED;
        rejectionReason = ServicePublicationRequest.normalizeNullable(reason);
    }

    private void review(Long reviewer, Instant now) {
        requirePending();
        positive(reviewer, "reviewedByUserId");
        Objects.requireNonNull(now, "now");
        if (now.isBefore(updatedAt)) throw new IllegalArgumentException("Review time cannot precede updatedAt.");
        reviewedByUserId = reviewer;
        reviewedAt = now;
        updatedAt = now;
    }

    private static void positive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be positive.");
    }

    public Long getId() { return id; }
    public Long getServicePublicationId() { return servicePublicationId; }
    public Long getRequestedByUserId() { return requestedByUserId; }
    public String getProposedTitle() { return proposedTitle; }
    public String getProposedContent() { return proposedContent; }
    public BigDecimal getProposedAmount() { return proposedAmount; }
    public String getProposedContactPhone() { return proposedContactPhone; }
    public List<PublicationImage> getProposedImages() { return proposedImages; }
    public ServicePublicationChangeRequestStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
