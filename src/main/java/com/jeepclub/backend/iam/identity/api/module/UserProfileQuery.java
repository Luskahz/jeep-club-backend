package com.jeepclub.backend.iam.identity.api.module;

/** Informational only; never a prerequisite for authentication or membership. */
public interface UserProfileQuery {
    /** Throws UserNotFoundException for an unknown user. */
    boolean hasPendingProfileCompletion(Long userId);
}
