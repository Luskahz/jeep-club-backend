package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Approval request: it is never itself a Publication. */
public final class ServicePublicationRequest {
    private final Long id;
    private final Long requestedByUserId;
    private final String title;
    private final String content;
    private final BigDecimal amount;
    private final String contactPhone;
    private final List<PublicationImage> images;
    private ServicePublicationRequestStatus status;
    private String rejectionReason;
    private Long reviewedByUserId;
    private Long createdPublicationId;
    private final Instant requestedAt;
    private Instant reviewedAt;
    private Instant updatedAt;
    private final Long version;

    private ServicePublicationRequest(Long id, Long requestedByUserId, String title, String content,
                                      BigDecimal amount, String contactPhone, List<PublicationImage> images,
                                      ServicePublicationRequestStatus status, String rejectionReason,
                                      Long reviewedByUserId, Long createdPublicationId, Instant requestedAt,
                                      Instant reviewedAt, Instant updatedAt, Long version) {
        if (id != null) positive(id, "id");
        positive(requestedByUserId, "requestedByUserId");
        if (version != null && version < 0) throw new IllegalArgumentException("version must be non-negative.");
        this.id = id;
        this.requestedByUserId = requestedByUserId;
        this.title = required(title, "title", 200);
        this.content = required(content, "content", Integer.MAX_VALUE);
        this.amount = requireAmount(amount);
        this.contactPhone = required(contactPhone, "contactPhone", 30);
        this.images = PublicationGallery.of(images).images();
        this.status = Objects.requireNonNull(status, "status");
        this.rejectionReason = normalizeNullable(rejectionReason);
        this.reviewedByUserId = reviewedByUserId;
        this.createdPublicationId = createdPublicationId;
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.reviewedAt = reviewedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
        if (updatedAt.isBefore(requestedAt) || (reviewedAt != null && (reviewedAt.isBefore(requestedAt) || updatedAt.isBefore(reviewedAt)))) {
            throw new IllegalArgumentException("Inconsistent request timestamps.");
        }
        if (status == ServicePublicationRequestStatus.PENDING) {
            if (reviewedByUserId != null || createdPublicationId != null || reviewedAt != null || rejectionReason != null) {
                throw new IllegalArgumentException("Pending request cannot contain review results.");
            }
        } else {
            positive(reviewedByUserId, "reviewedByUserId");
            Objects.requireNonNull(reviewedAt, "reviewedAt");
            if (status == ServicePublicationRequestStatus.APPROVED) {
                positive(createdPublicationId, "createdPublicationId");
                if (rejectionReason != null) throw new IllegalArgumentException("Approved request cannot have a rejection reason.");
            } else if (createdPublicationId != null) {
                throw new IllegalArgumentException("Rejected request cannot create a publication.");
            }
        }
    }

    public static ServicePublicationRequest create(Long requestedByUserId, String title, String content,
                                                   BigDecimal amount, String contactPhone,
                                                   List<PublicationImage> images, Instant now) {
        Objects.requireNonNull(now, "now");
        return new ServicePublicationRequest(null, requestedByUserId, title, content, amount, contactPhone, images,
                ServicePublicationRequestStatus.PENDING, null, null, null, now, null, now, null);
    }

    public static ServicePublicationRequest reconstitute(Long id, Long requestedByUserId, String title, String content,
                                                         BigDecimal amount, String contactPhone, List<PublicationImage> images,
                                                         ServicePublicationRequestStatus status, String rejectionReason,
                                                         Long reviewedByUserId, Long createdPublicationId, Instant requestedAt,
                                                         Instant reviewedAt, Instant updatedAt, Long version) {
        positive(id, "id");
        Objects.requireNonNull(version, "version");
        return new ServicePublicationRequest(id, requestedByUserId, title, content, amount, contactPhone, images,
                status, rejectionReason, reviewedByUserId, createdPublicationId, requestedAt, reviewedAt, updatedAt, version);
    }

    public void requirePending() {
        if (status != ServicePublicationRequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been reviewed: " + status);
        }
    }

    public void approve(Long reviewedByUserId, Long createdPublicationId, Instant now) {
        requirePending();
        positive(reviewedByUserId, "reviewedByUserId");
        positive(createdPublicationId, "createdPublicationId");
        requireReviewTime(now);
        status = ServicePublicationRequestStatus.APPROVED;
        this.reviewedByUserId = reviewedByUserId;
        this.createdPublicationId = createdPublicationId;
        reviewedAt = now;
        updatedAt = now;
        rejectionReason = null;
    }

    public void reject(Long reviewedByUserId, String rejectionReason, Instant now) {
        requirePending();
        positive(reviewedByUserId, "reviewedByUserId");
        requireReviewTime(now);
        status = ServicePublicationRequestStatus.REJECTED;
        this.reviewedByUserId = reviewedByUserId;
        this.rejectionReason = normalizeNullable(rejectionReason);
        reviewedAt = now;
        updatedAt = now;
    }

    private void requireReviewTime(Instant now) {
        Objects.requireNonNull(now, "now");
        if (now.isBefore(updatedAt)) throw new IllegalArgumentException("Review time cannot precede updatedAt.");
    }

    static String required(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required.");
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) throw new IllegalArgumentException(name + " is too long.");
        return trimmed;
    }

    static BigDecimal requireAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive.");
        if (amount.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("amount supports at most two decimal places.");
        BigDecimal normalized = amount.setScale(2);
        if (normalized.precision() > 15) throw new IllegalArgumentException("amount exceeds storage precision.");
        return normalized;
    }

    static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void positive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be positive.");
    }

    public Long getId() { return id; }
    public Long getRequestedByUserId() { return requestedByUserId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public BigDecimal getAmount() { return amount; }
    public String getContactPhone() { return contactPhone; }
    public List<PublicationImage> getImages() { return images; }
    public ServicePublicationRequestStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
    public Long getCreatedPublicationId() { return createdPublicationId; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
