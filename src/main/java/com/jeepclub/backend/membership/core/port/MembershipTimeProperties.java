package com.jeepclub.backend.membership.core.port;

import java.time.Duration;

public interface MembershipTimeProperties {
    Duration activationTokenTtl();
}