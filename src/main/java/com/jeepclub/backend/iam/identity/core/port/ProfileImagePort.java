package com.jeepclub.backend.iam.identity.core.port;

/** Verifies a reference from the global image catalog; null means no association. */
public interface ProfileImagePort {
    void requireExisting(String storageKey);
}
