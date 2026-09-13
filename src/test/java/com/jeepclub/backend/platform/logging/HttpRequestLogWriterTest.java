package com.jeepclub.backend.platform.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLogWriterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(HttpRequestLogWriter.LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void attachAppender() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void emitsOneCompactLineWithSeverityDerivedFromHttpResult() {
        var writer = new HttpRequestLogWriter();

        writer.write(event(200, false));
        writer.write(event(404, false));
        writer.write(event(503, false));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.WARN, Level.ERROR);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> {
                    assertThat(message).contains(
                            "http_request requestId=request-123",
                            "method=GET",
                            "path=/vehicles/{vehicleId}",
                            "durationMs=12",
                            "device=WEB",
                            "userId=42",
                            "userName=\"Lucas Alves\""
                    );
                    assertThat(message).doesNotContain("\n", "\r");
                });
    }

    @Test
    void treatsUnhandledFailureAsErrorEvenBeforeContainerCommitsFiveHundredStatus() {
        new HttpRequestLogWriter().write(event(500, true));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    private HttpRequestLogEvent event(int status, boolean unhandledFailure) {
        return new HttpRequestLogEvent(
                "request-123",
                "GET",
                "/vehicles/{vehicleId}",
                status,
                12,
                "WEB",
                "42",
                "Lucas Alves",
                unhandledFailure
        );
    }
}
