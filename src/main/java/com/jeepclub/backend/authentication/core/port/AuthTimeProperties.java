package com.jeepclub.backend.authentication.core.port;

import java.time.Duration;

public interface AuthTimeProperties {
    Duration sessionTtl();
    Duration refreshTokenTtl();
    Duration passwordChangeRequestTtl();
}
