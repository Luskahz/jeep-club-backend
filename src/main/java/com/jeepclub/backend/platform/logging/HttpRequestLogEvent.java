package com.jeepclub.backend.platform.logging;

record HttpRequestLogEvent(
        String method,
        String path,
        int status,
        long durationMillis,
        boolean unhandledFailure
) {
}
