package com.jeepclub.backend.platform.logging;

public interface SystemLogService {
    void record(SystemLogEvent event);
    /** Durably records a sensitive access before returning its protected data. */
    void recordRequired(SystemLogEvent event);
}
