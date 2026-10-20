package com.jeepclub.backend.iam.identity.api.module.spi;
/** Called under the Identity row lock, in the deleting transaction. Own BC current reads only. */
public interface UserDeletionGuard {
    boolean hasRetainedReferences(Long userId);
}
