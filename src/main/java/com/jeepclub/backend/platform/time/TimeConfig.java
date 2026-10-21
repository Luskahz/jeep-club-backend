package com.jeepclub.backend.platform.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return databasePrecision(Clock.systemUTC());
    }

    // Flyway/JPA persist DATETIME(6). Flooring before a write prevents database
    // rounding from making createdAt appear later than a subsequent clock read.
    static Clock databasePrecision(Clock source) {
        return Clock.tick(source, Duration.ofNanos(1_000));
    }
}
