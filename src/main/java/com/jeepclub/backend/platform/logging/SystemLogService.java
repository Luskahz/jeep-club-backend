package com.jeepclub.backend.platform.logging;

public interface SystemLogService {
    void record(SystemLogEvent event);
    /** Durably records a sensitive access before returning its protected data. */
    void recordRequired(SystemLogEvent event);
    /** Persists an audit event atomically with the current business transaction. */
    void recordInTransaction(SystemLogEvent event);
}
