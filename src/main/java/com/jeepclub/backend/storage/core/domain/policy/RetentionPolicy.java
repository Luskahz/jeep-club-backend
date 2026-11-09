package com.jeepclub.backend.storage.core.domain.policy;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaAsset;
import com.jeepclub.backend.storage.core.domain.model.MediaReference;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Dry-run policy only. Its result is never authority to erase physical objects. */
public final class RetentionPolicy {
    public enum Decision { ACTIVE, LEGAL_HOLD, INDETERMINATE, WAITING, ELIGIBLE_FOR_REVIEW }
    public enum RetentionClass { COMMON, SENSITIVE }
    public record Assessment(Decision decision, RetentionClass retentionClass, Instant unusedSince, Instant reviewAfter) {}
    private final Duration common;
    private final Duration sensitive;
    public RetentionPolicy(long commonDays, long sensitiveDays) {
        if (commonDays <= 0 || sensitiveDays <= 0) throw new IllegalArgumentException("Retention days must be positive");
        common = Duration.ofDays(commonDays); sensitive = Duration.ofDays(sensitiveDays);
    }
    /** The caller must supply the complete reference history and a known legal-hold state. */
    public Assessment assess(MediaAsset asset, List<MediaReference> history, boolean completeHistory,
                             Boolean legalHold, Instant now) {
        Objects.requireNonNull(asset); Objects.requireNonNull(history); Objects.requireNonNull(now);
        if (Boolean.TRUE.equals(legalHold)) return new Assessment(Decision.LEGAL_HOLD, null, null, null);
        if (legalHold == null || !completeHistory || asset.mediaId() == null || asset.createdAt().isAfter(now))
            return new Assessment(Decision.INDETERMINATE, null, null, null);
        RetentionClass kind = classify(asset.sourceContext());
        Instant unusedSince = asset.createdAt();
        boolean active = false;
        for (MediaReference ref : history) {
            if (!asset.mediaId().equals(ref.mediaId()) || ref.linkedAt().isBefore(asset.createdAt()) || ref.linkedAt().isAfter(now)
                    || (ref.unlinkedAt() != null && ref.unlinkedAt().isAfter(now)))
                return new Assessment(Decision.INDETERMINATE, null, null, null);
            RetentionClass refKind = classify(ref.context());
            if (refKind == RetentionClass.SENSITIVE) kind = refKind;
            if (ref.unlinkedAt() == null) { active = true; continue; }
            if (ref.unlinkedAt().isAfter(unusedSince)) unusedSince = ref.unlinkedAt();
        }
        if (active) return new Assessment(Decision.ACTIVE, kind, null, null);
        if (kind == null) return new Assessment(Decision.INDETERMINATE, null, null, null);
        Instant after = unusedSince.plus(kind == RetentionClass.SENSITIVE ? sensitive : common);
        return new Assessment(now.isBefore(after) ? Decision.WAITING : Decision.ELIGIBLE_FOR_REVIEW, kind, unusedSince, after);
    }
    private static RetentionClass classify(MediaContext context) {
        return switch (context) {
            case DEPENDENT_PHOTO, MEDICAL_IMAGE, IDENTIFICATION_PHOTO -> RetentionClass.SENSITIVE;
            case IDENTITY_PROFILE, VEHICLE_PHOTO, PUBLICATION_IMAGE, PUBLICATION_COMMENT, EVENT_IMAGE, TOOL_IMAGE -> RetentionClass.COMMON;
            case UNKNOWN -> null;
        };
    }
}
