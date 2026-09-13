package com.jeepclub.backend.platform.logging;

record HttpRequestLogEvent(
        String requestId,
        String method,
        String path,
        int status,
        long durationMillis,
        String device,
        String userId,
        String userName,
        boolean unhandledFailure
) {
}
