package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.model.*;
import java.time.Instant;

public interface MediaGovernance {
    void initialize(long mediaId, String receivedSha256, boolean sensitive, MediaAuditContext audit);
    boolean allows(long mediaId, long referenceId);
    void moderate(ModerationDecision decision);
    void previewAccess(long mediaId,long referenceId,MediaAuditContext audit);
    void legalHold(long mediaId, boolean held, MediaAuditContext audit);
}
