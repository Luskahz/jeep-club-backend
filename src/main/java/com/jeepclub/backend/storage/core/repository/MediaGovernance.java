package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.model.*;
import java.time.Instant;

public interface MediaGovernance {
    enum PreviewPurpose { MODERATION_REVIEW, USER_REQUEST, TECHNICAL_INVESTIGATION }
    void initialize(long mediaId, String receivedSha256, boolean sensitive, MediaAuditContext audit);
    boolean allows(long mediaId, long referenceId);
    void moderate(ModerationDecision decision);
    void previewAccess(long mediaId,long referenceId,PreviewPurpose purpose,MediaAuditContext audit);
    void legalHold(long mediaId, boolean held, String justification, MediaAuditContext audit);
}
