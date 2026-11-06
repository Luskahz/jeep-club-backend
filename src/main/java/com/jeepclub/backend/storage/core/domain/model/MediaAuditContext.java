package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.MediaActorKind;
import java.time.Instant;
import java.util.Objects;

/** Only structural audit attributes; no arbitrary notes, filenames, paths or PII. */
public record MediaAuditContext(MediaActorKind actorKind, Long actorUserId, Instant occurredAt, String correlationId) {
    public MediaAuditContext {
        Objects.requireNonNull(actorKind); Objects.requireNonNull(occurredAt);
        MediaInvariants.id(actorUserId, "audit actor");
        MediaInvariants.require((actorKind == MediaActorKind.USER) == (actorUserId != null), "audit actor provenance");
        MediaInvariants.optionalToken(correlationId, 64, "correlationId");
    }
    @Override public String toString() { return "MediaAuditContext[actorKind=" + actorKind + "]"; }
}
