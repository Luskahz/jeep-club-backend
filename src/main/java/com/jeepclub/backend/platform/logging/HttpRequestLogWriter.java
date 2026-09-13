package com.jeepclub.backend.platform.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class HttpRequestLogWriter {

    static final String LOGGER_NAME = "com.jeepclub.backend.platform.logging.http";
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    void write(HttpRequestLogEvent event) {
        String message = "http_request requestId={} method={} path={} status={} durationMs={} "
                + "device={} userId={} userName=\"{}\"";
        Object[] arguments = {
                event.requestId(),
                event.method(),
                event.path(),
                event.status(),
                event.durationMillis(),
                event.device(),
                event.userId(),
                HttpLogValueSanitizer.quoted(event.userName())
        };

        if (event.unhandledFailure() || event.status() >= 500) {
            LOGGER.error(message, arguments);
        } else if (event.status() >= 400) {
            LOGGER.warn(message, arguments);
        } else {
            LOGGER.info(message, arguments);
        }
    }

}
