package com.jeepclub.backend.platform.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
        MDC.clear();
    }

    @Test
    void emitsOneCompactLineWithSeverityDerivedFromHttpResult() {
        var writer = new HttpRequestLogWriter();
        MDC.put(HttpLoggingContext.REQUEST_ID, "request-123");
        MDC.put(HttpLoggingContext.USER_ID, "42");
        MDC.put(HttpLoggingContext.USER_NAME, "Lucas Alves");
        MDC.put(HttpLoggingContext.DEVICE, "WEB");

        writer.write(event(200, false));
        writer.write(event(404, false));
        writer.write(event(503, false));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.WARN, Level.ERROR);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> {
                    assertThat(message).contains(
                            "http_request",
                            "method=GET",
                            "path=/vehicles/{vehicleId}",
                            "durationMs=12"
                    );
                    assertThat(message).doesNotContain(
                            "requestId=", "device=", "userId=", "userName="
                    );
                    assertThat(message).doesNotContain("\n", "\r");
                });
        assertThat(appender.list).allSatisfy(event ->
                assertThat(event.getMDCPropertyMap())
                        .containsEntry("requestId", "request-123")
                        .containsEntry("userId", "42")
                        .containsEntry("userName", "Lucas Alves")
                        .containsEntry("device", "WEB"));
    }

    @Test
    void treatsUnhandledFailureAsErrorEvenBeforeContainerCommitsFiveHundredStatus() {
        new HttpRequestLogWriter().write(event(500, true));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void controlledHttpFieldsCannotCreateAdditionalLogLines() {
        new HttpRequestLogWriter().write(new HttpRequestLogEvent(
                "GET", "/vehicles_Injected", 200, 1, false
        ));

        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("path=/vehicles_Injected");
        assertThat(message).doesNotContain("\n", "\r");
    }

    private HttpRequestLogEvent event(int status, boolean unhandledFailure) {
        return new HttpRequestLogEvent(
                "GET",
                "/vehicles/{vehicleId}",
                status,
                12,
                unhandledFailure
        );
    }
}
