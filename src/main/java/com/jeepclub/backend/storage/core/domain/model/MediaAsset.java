package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.*;
import java.time.Instant;
import java.util.Objects;

/** Logical identity. Classification records metadata review, never permission to read bytes. */
public record MediaAsset(Long mediaId, MediaOrigin origin, MediaContext sourceContext,
        String sourceRecordId, Long uploadedByUserId, Long ownerUserId,
        MediaSubjectType subjectType, Long subjectId, String purpose,
        MediaClassification classification, MediaPhysicalState physicalState,
        Instant createdAt, Instant updatedAt, Long version) {
    public MediaAsset {
        MediaInvariants.id(mediaId, "mediaId"); MediaInvariants.version(version);
        Objects.requireNonNull(origin); Objects.requireNonNull(sourceContext);
        Objects.requireNonNull(subjectType); Objects.requireNonNull(classification); Objects.requireNonNull(physicalState);
        MediaInvariants.optionalToken(sourceRecordId, 128, "sourceRecordId");
        MediaInvariants.id(uploadedByUserId, "uploader"); MediaInvariants.id(ownerUserId, "owner");
        MediaInvariants.id(subjectId, "subject");
        MediaInvariants.require((subjectType == MediaSubjectType.UNKNOWN) == (subjectId == null), "subject provenance");
        MediaInvariants.token(purpose, 64, "purpose");
        MediaInvariants.dates(createdAt, updatedAt);
        MediaInvariants.require((mediaId == null) == (version == null), "asset identity/version");
    }
    public static MediaAsset create(MediaOrigin origin, MediaContext context, String sourceRecordId,
            Long uploader, Long owner, MediaSubjectType subjectType, Long subjectId, String purpose, Instant now) {
        return new MediaAsset(null, origin, context, sourceRecordId, uploader, owner, subjectType, subjectId,
                purpose, MediaClassification.UNCLASSIFIED, MediaPhysicalState.UNKNOWN, now, now, null);
    }
    public MediaAsset revisePurpose(String purpose, Instant now) {
        MediaInvariants.require(!now.isBefore(updatedAt), "update time");
        return new MediaAsset(mediaId, origin, sourceContext, sourceRecordId, uploadedByUserId, ownerUserId,
                subjectType, subjectId, purpose, classification, physicalState, createdAt, now, version);
    }
    @Override public String toString() { return "MediaAsset[mediaId=" + mediaId + "]"; }
}
