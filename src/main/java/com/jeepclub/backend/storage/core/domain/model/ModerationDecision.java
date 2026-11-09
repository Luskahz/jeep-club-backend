package com.jeepclub.backend.storage.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/** A decision applies to one asset or to one reference, never to its shared physical bytes. */
public record ModerationDecision(long mediaId, Long referenceId, State state, Reason reason,
                                 String internalNote, long actorUserId, Instant occurredAt) {
    public enum State { VISIBLE, UNDER_REVIEW, HIDDEN, BLOCKED }
    public enum Reason { POLICY_VIOLATION, PRIVACY, INVALID_CONTENT, OWNER_REQUEST, OTHER, RESTORED }
    public ModerationDecision {
        if (mediaId <= 0 || (referenceId != null && referenceId <= 0) || actorUserId <= 0)
            throw new IllegalArgumentException("Invalid moderation identity");
        Objects.requireNonNull(state); Objects.requireNonNull(reason); Objects.requireNonNull(occurredAt);
        if (internalNote != null && (internalNote.isBlank() || internalNote.length() > 2000)) throw new IllegalArgumentException("Invalid moderation note");
        if ((state == State.VISIBLE) != (reason == Reason.RESTORED)) throw new IllegalArgumentException("Invalid restoration reason");
    }
    /** Internal notes are intentionally omitted from this safe representation. */
    public PublicView publicView() { return new PublicView(state); }
    public record PublicView(State state) {}
    @Override public String toString() { return "ModerationDecision[mediaId=" + mediaId + ", state=" + state + "]"; }
}
