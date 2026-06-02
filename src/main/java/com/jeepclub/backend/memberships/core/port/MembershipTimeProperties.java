package com.jeepclub.backend.memberships.core.port;

import java.time.Duration;

public interface MembershipTimeProperties {
    Duration activationTokenTtl();
}