package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.*;
import java.time.Instant;
import java.util.Objects;

/** Historical interval. Ending one interval never removes the asset or another interval. */
public record MediaReference(Long referenceId, Long mediaId, MediaContext context, String consumerId,
        String role, Integer position, MediaOrigin associationOrigin, Instant linkedAt,
        Long linkedByUserId, Instant unlinkedAt, Long unlinkedByUserId, String unlinkReasonCode, Long version) {
    public MediaReference {
        MediaInvariants.id(referenceId, "referenceId"); MediaInvariants.requiredId(mediaId, "mediaId");
        Objects.requireNonNull(context); Objects.requireNonNull(associationOrigin); Objects.requireNonNull(linkedAt);
        MediaInvariants.require(context != MediaContext.UNKNOWN, "reference context");
        MediaInvariants.token(consumerId, 128, "consumerId"); MediaInvariants.token(role, 64, "reference role");
        MediaInvariants.require(position == null || position >= 0, "position");
        MediaInvariants.id(linkedByUserId, "link actor"); MediaInvariants.id(unlinkedByUserId, "unlink actor");
        MediaInvariants.optionalToken(unlinkReasonCode, 64, "unlinkReasonCode");
        MediaInvariants.require(unlinkedAt == null ? unlinkedByUserId == null && unlinkReasonCode == null
                : !unlinkedAt.isBefore(linkedAt), "historical interval");
        MediaInvariants.version(version);
        MediaInvariants.require((referenceId == null) == (version == null), "reference identity/version");
    }
    public MediaReference end(Instant at, Long actor, String reasonCode) {
        MediaInvariants.require(unlinkedAt == null && at != null, "reference already ended or missing end time");
        return new MediaReference(referenceId, mediaId, context, consumerId, role, position, associationOrigin,
                linkedAt, linkedByUserId, at, actor, reasonCode, version);
    }
    public boolean isCurrent() { return unlinkedAt == null; }
    @Override public String toString() { return "MediaReference[referenceId=" + referenceId + ", mediaId=" + mediaId + "]"; }
}
