package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleTest {

    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void normalizePlateTrimsAndUppercases() {
        assertThat(Vehicle.normalizePlate("  abc1d23  ")).isEqualTo("ABC1D23");
        assertThat(Vehicle.normalizePlate("AbC1d23")).isEqualTo("ABC1D23");
        assertThat(Vehicle.normalizePlate("ABC1D23")).isEqualTo("ABC1D23");
        assertThat(Vehicle.normalizePlate(null)).isNull();
    }

    @Test
    void normalizeRenavamKeepsOnlyDigits() {
        assertThat(Vehicle.normalizeRenavam("382.492.064-28")).isEqualTo("38249206428");
        assertThat(Vehicle.normalizeRenavam("38249206428")).isEqualTo("38249206428");
        assertThat(Vehicle.normalizeRenavam(null)).isNull();
    }

    @Test
    void createCanonicalizesPlateAndRenavam() {
        Vehicle vehicle = Vehicle.create(
                "Trovão", "photo", "  abc1d23  ", "382.492.064-28", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, true, 7L, NOW
        );

        assertThat(vehicle.getPlate()).isEqualTo("ABC1D23");
        assertThat(vehicle.getRenavam()).isEqualTo("38249206428");
    }

    @Test
    void updateCanonicalizesPlateAndRenavam() {
        Vehicle vehicle = Vehicle.reconstitute(
                1L, "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, VehicleStatus.ACTIVE, true, 7L, NOW, null
        );

        vehicle.update(
                "Novo", "photo", "  xyz9z99  ", "123.456.789-01", "Jeep",
                "Renegade", 2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true,
                NOW.plusSeconds(60)
        );

        assertThat(vehicle.getPlate()).isEqualTo("XYZ9Z99");
        assertThat(vehicle.getRenavam()).isEqualTo("12345678901");
    }

    @Test
    void reconstituteCanonicalizesLegacyNonCanonicalValues() {
        Vehicle vehicle = Vehicle.reconstitute(
                1L, "Trovão", "photo", "abc1d23", "382.492.064-28", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, VehicleStatus.ACTIVE, true, 7L, NOW, null
        );

        assertThat(vehicle.getPlate()).isEqualTo("ABC1D23");
        assertThat(vehicle.getRenavam()).isEqualTo("38249206428");
    }
}
