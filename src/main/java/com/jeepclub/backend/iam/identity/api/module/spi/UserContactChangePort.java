package com.jeepclub.backend.iam.identity.api.module.spi;

import java.time.Instant;

/** Invalidates pending links owned by the provider without rewriting historical applications. */
public interface UserContactChangePort {
    void invalidatePendingLinks(Long userId, Instant now);
}
