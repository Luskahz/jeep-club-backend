package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.MediaAuditOperation;
import java.util.Objects;

public record MediaAuditEvent(Long eventId, Long mediaId, Long variantId, Long referenceId,
        MediaAuditOperation operation, MediaAuditContext actor, Long previousVersion, Long resultingVersion) {
    public MediaAuditEvent {
        MediaInvariants.id(eventId, "eventId"); MediaInvariants.requiredId(mediaId, "mediaId");
        MediaInvariants.id(variantId, "variantId"); MediaInvariants.id(referenceId, "referenceId");
        Objects.requireNonNull(operation); Objects.requireNonNull(actor);
        MediaInvariants.version(previousVersion); MediaInvariants.version(resultingVersion);
        MediaInvariants.require(!(variantId != null && referenceId != null), "audit target");
    }
    @Override public String toString() { return "MediaAuditEvent[eventId=" + eventId + ", operation=" + operation + "]"; }
}
