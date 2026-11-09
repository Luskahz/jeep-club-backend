package com.jeepclub.backend.storage.core.domain.policy;

import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RetentionPolicyTest {
    private static final Instant T = Instant.parse("2026-01-01T00:00:00Z");
    private final RetentionPolicy policy = new RetentionPolicy(365, 90);
    private MediaAsset asset(MediaContext source) { return new MediaAsset(1L, MediaOrigin.UPLOAD, source, null, 1L, 1L, MediaSubjectType.USER, 1L, "PHOTO", MediaClassification.UNCLASSIFIED, MediaPhysicalState.PRESENT, T, T, 0L); }
    private MediaReference ref(MediaContext context, Instant end) { return new MediaReference(1L, 1L, context, "1", "PHOTO", null, MediaOrigin.UPLOAD, T, 1L, end, end == null ? null : 1L, end == null ? null : "REPLACED", 0L); }
    @Test void sensitiveHistoryWinsAndStartsAtLastUnlink() {
        var result = policy.assess(asset(MediaContext.IDENTITY_PROFILE), List.of(ref(MediaContext.DEPENDENT_PHOTO, T.plusSeconds(86400))), true, false, T.plusSeconds(91L*86400));
        assertThat(result.retentionClass()).isEqualTo(RetentionPolicy.RetentionClass.SENSITIVE);
        assertThat(result.decision()).isEqualTo(RetentionPolicy.Decision.ELIGIBLE_FOR_REVIEW);
        assertThat(result.unusedSince()).isEqualTo(T.plusSeconds(86400));
    }
    @Test void activeHoldAndIncompleteHistoryNeverBecomeCandidates() {
        assertThat(policy.assess(asset(MediaContext.IDENTITY_PROFILE), List.of(ref(MediaContext.VEHICLE_PHOTO, null)), true, false, T.plusSeconds(400L*86400)).decision()).isEqualTo(RetentionPolicy.Decision.ACTIVE);
        assertThat(policy.assess(asset(MediaContext.IDENTITY_PROFILE), List.of(), true, true, T.plusSeconds(400L*86400)).decision()).isEqualTo(RetentionPolicy.Decision.LEGAL_HOLD);
        assertThat(policy.assess(asset(MediaContext.IDENTITY_PROFILE), List.of(), false, false, T.plusSeconds(400L*86400)).decision()).isEqualTo(RetentionPolicy.Decision.INDETERMINATE);
        assertThat(policy.assess(asset(MediaContext.UNKNOWN), List.of(), true, false, T.plusSeconds(400L*86400)).decision()).isEqualTo(RetentionPolicy.Decision.INDETERMINATE);
    }
    @Test void neverLinkedStartsAtUploadAndDeadlineIsInclusive() {
        assertThat(policy.assess(asset(MediaContext.VEHICLE_PHOTO), List.of(), true, false, T.plusSeconds(365L*86400-1)).decision()).isEqualTo(RetentionPolicy.Decision.WAITING);
        assertThat(policy.assess(asset(MediaContext.VEHICLE_PHOTO), List.of(), true, false, T.plusSeconds(365L*86400)).decision()).isEqualTo(RetentionPolicy.Decision.ELIGIBLE_FOR_REVIEW);
    }
}
