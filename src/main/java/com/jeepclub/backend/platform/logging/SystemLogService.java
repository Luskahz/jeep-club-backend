package com.jeepclub.backend.platform.logging;

public interface SystemLogService {
    void record(SystemLogEvent event);
}
