package com.jeepclub.backend.iam.identity.api.module.spi;
import java.time.Instant;
/** Only ephemeral onboarding associations may be archived/removed, atomically with Identity deletion. */
public interface UserDeletionCleanup {
    void removePendingAssociations(Long userId, Long actorId, Instant now);
}
