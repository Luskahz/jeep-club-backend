package com.jeepclub.backend.iam.authentication.core.port;

import java.time.Duration;

public interface ApplicationTimeProperties {
    Duration sessionTtl();
    Duration refreshTokenTtl();
    Duration passwordRecoveryRequestTtl();
    Duration passwordChangeChallengeTtl();
}
