package com.jeepclub.backend.memberships.core.port;

public interface CompletePendingFirstAccessPort {
    void complete(Long identityId, String newPassword);
}
