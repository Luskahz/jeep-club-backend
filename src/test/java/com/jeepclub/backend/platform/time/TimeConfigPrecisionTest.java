package com.jeepclub.backend.platform.time;

import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigPrecisionTest {
    @Test void databaseRoundTripCannotPutCreationAfterTheSameSourceClockRead() throws Exception {
        var instant = Instant.parse("2026-10-19T10:00:00.123456700Z");
        var source = Clock.fixed(instant, ZoneOffset.UTC);
        var clock = TimeConfig.databasePrecision(source);
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:clock_precision;MODE=MySQL", "sa", "")) {
            connection.createStatement().execute("CREATE TABLE timestamps (id INT, created_at TIMESTAMP(6) WITH TIME ZONE)");
            try (var insert = connection.prepareStatement("INSERT INTO timestamps VALUES (?, ?)");) {
                insert.setInt(1, 1); insert.setObject(2, instant.atOffset(ZoneOffset.UTC)); insert.executeUpdate();
                insert.setInt(1, 2); insert.setObject(2, clock.instant().atOffset(ZoneOffset.UTC)); insert.executeUpdate();
            }
            try (var rows = connection.createStatement().executeQuery("SELECT id, created_at FROM timestamps ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                var unnormalized = rows.getObject(2, OffsetDateTime.class).toInstant();
                assertThat(unnormalized).isAfter(source.instant()); // Negative control reproduces the failure.
                assertThat(rows.next()).isTrue();
                var persisted = rows.getObject(2, OffsetDateTime.class).toInstant();
                assertThat(persisted).isEqualTo(clock.instant()).isBeforeOrEqualTo(source.instant());
            }
        }
    }

    @Test void precisionPreservesTheUtcInstantAcrossZonesAndDoesNotRoundForward() {
        var source = Clock.fixed(Instant.parse("2026-10-19T10:00:00.999999900Z"), ZoneOffset.UTC);
        var clock = TimeConfig.databasePrecision(source);
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-10-19T10:00:00.999999Z"));
        assertThat(clock.withZone(ZoneId.of("America/Sao_Paulo")).instant()).isEqualTo(clock.instant());
        assertThat(new TimeConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
