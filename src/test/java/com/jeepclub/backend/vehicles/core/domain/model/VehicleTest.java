package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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

    @Test
    void reconstituteAcceptsLegacySoftDeletedStatus() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs()
                .status(VehicleStatus.SOFT_DELETED)
                .build();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOFT_DELETED);
    }

    @Test
    void reconstituteAcceptsNullUpdatedAt() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs()
                .updatedAt(null)
                .build();

        assertThat(vehicle.getUpdatedAt()).isNull();
    }

    // --- create: invariantes obrigatórias ---

    @Test
    void createRejectsNullOwnerId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().ownerId(null).build());
    }

    @Test
    void createRejectsNonPositiveOwnerId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().ownerId(0L).build());
    }

    @Test
    void createRejectsBlankPlate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().plate("   ").build());
    }

    @Test
    void createRejectsBlankRenavam() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().renavam(null).build());
    }

    @Test
    void createRejectsRenavamThatCanonicalizesToBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().renavam("---abc---").build());
    }

    @Test
    void reconstituteRejectsRenavamThatCanonicalizesToBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().renavam("---abc---").build());
    }

    @Test
    void updateRejectsRenavamThatCanonicalizesToBlank() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatIllegalArgumentException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "---abc---", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void createRejectsBlankBrand() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().brand(" ").build());
    }

    @Test
    void createRejectsBlankModel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().model(null).build());
    }

    @Test
    void createRejectsBlankColor() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().color(" ").build());
    }

    @Test
    void createRejectsManufacturingYearBeforeMinimum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().manufacturingYear(1899).build());
    }

    @Test
    void createRejectsModelYearBeforeMinimum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().modelYear(1899).build());
    }

    @Test
    void createRejectsSeatingCapacityBelowOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().seatingCapacity(0).build());
    }

    @Test
    void createRejectsNegativeEngineDisplacement() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().engineDisplacement(-0.1).build());
    }

    @Test
    void createAcceptsZeroEngineDisplacement() {
        Vehicle vehicle = new CreateArgs().engineDisplacement(0.0).build();

        assertThat(vehicle.getEngineDisplacement()).isZero();
    }

    @Test
    void createRejectsNullFuelType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().fuelType(null).build());
    }

    @Test
    void createRejectsNullTowing() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().towing(null).build());
    }

    @Test
    void createRejectsNullCreatedAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().createdAt(null).build());
    }

    @Test
    void createProducesActiveStatus() {
        Vehicle vehicle = new CreateArgs().build();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getId()).isNull();
        assertThat(vehicle.getUpdatedAt()).isNull();
    }

    // --- reconstitute: invariantes obrigatórias ---

    @Test
    void reconstituteRejectsNonPositiveId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().id(0L).build());
    }

    @Test
    void reconstituteRejectsNonPositiveOwnerId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().ownerId(-1L).build());
    }

    @Test
    void reconstituteRejectsBlankBrand() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().brand("").build());
    }

    @Test
    void reconstituteRejectsManufacturingYearBeforeMinimum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().manufacturingYear(1899).build());
    }

    @Test
    void reconstituteRejectsSeatingCapacityBelowOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().seatingCapacity(0).build());
    }

    @Test
    void reconstituteRejectsNegativeEngineDisplacement() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().engineDisplacement(-2.0).build());
    }

    @Test
    void reconstituteRejectsNullFuelType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().fuelType(null).build());
    }

    @Test
    void reconstituteRejectsNullStatus() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().status(null).build());
    }

    @Test
    void reconstituteRejectsNullTowing() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().towing(null).build());
    }

    @Test
    void reconstituteRejectsNullCreatedAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs().createdAt(null).build());
    }

    @Test
    void reconstituteRejectsUpdatedAtBeforeCreatedAt() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new CreateArgs().reconstituteArgs()
                        .updatedAt(NOW.minusSeconds(1))
                        .build());
    }

    // --- update: invariantes obrigatórias ---

    @Test
    void updateRejectsBlankPlate() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatIllegalArgumentException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "   ", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsBlankBrand() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatIllegalArgumentException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", " ", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsSeatingCapacityBelowOne() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatIllegalArgumentException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 0, FuelType.FLEX, 1.8, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsNegativeEngineDisplacement() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatIllegalArgumentException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, -1.0, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsNullFuelType() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatNullPointerException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, null, 1.8, true, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsNullTowing() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatNullPointerException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, null, NOW.plusSeconds(60)
        ));
    }

    @Test
    void updateRejectsNullNow() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        assertThatNullPointerException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true, null
        ));
    }

    @Test
    void updateRejectsNowBeforeCreatedAt() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().createdAt(NOW).build();

        assertThatIllegalStateException().isThrownBy(() -> vehicle.update(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true, NOW.minusSeconds(1)
        ));
    }

    @Test
    void updateAcceptsNullColorAndNickname() {
        Vehicle vehicle = new CreateArgs().reconstituteArgs().build();

        vehicle.update(
                null, null, "ABC1D23", "38249206428", "Jeep", "Wrangler",
                2023, 2024, null, 5, FuelType.FLEX, 1.8, true, NOW.plusSeconds(60)
        );

        assertThat(vehicle.getNickname()).isNull();
        assertThat(vehicle.getColor()).isNull();
    }

    /** Builder de teste com valores padrão válidos para {@link Vehicle#create}. */
    private static final class CreateArgs {
        String nickname = "Trovão";
        String photo = "photo";
        String plate = "ABC1D23";
        String renavam = "38249206428";
        String brand = "Jeep";
        String model = "Wrangler";
        int manufacturingYear = 2023;
        int modelYear = 2024;
        String color = "Verde";
        int seatingCapacity = 5;
        FuelType fuelType = FuelType.DIESEL;
        double engineDisplacement = 2.0;
        Boolean towing = true;
        Long ownerId = 7L;
        Instant createdAt = NOW;

        CreateArgs ownerId(Long value) { this.ownerId = value; return this; }
        CreateArgs plate(String value) { this.plate = value; return this; }
        CreateArgs renavam(String value) { this.renavam = value; return this; }
        CreateArgs brand(String value) { this.brand = value; return this; }
        CreateArgs model(String value) { this.model = value; return this; }
        CreateArgs color(String value) { this.color = value; return this; }
        CreateArgs manufacturingYear(int value) { this.manufacturingYear = value; return this; }
        CreateArgs modelYear(int value) { this.modelYear = value; return this; }
        CreateArgs seatingCapacity(int value) { this.seatingCapacity = value; return this; }
        CreateArgs engineDisplacement(double value) { this.engineDisplacement = value; return this; }
        CreateArgs fuelType(FuelType value) { this.fuelType = value; return this; }
        CreateArgs towing(Boolean value) { this.towing = value; return this; }
        CreateArgs createdAt(Instant value) { this.createdAt = value; return this; }

        Vehicle build() {
            return Vehicle.create(
                    nickname, photo, plate, renavam, brand, model, manufacturingYear,
                    modelYear, color, seatingCapacity, fuelType, engineDisplacement,
                    towing, ownerId, createdAt
            );
        }

        ReconstituteArgs reconstituteArgs() {
            return new ReconstituteArgs(this);
        }
    }

    /** Builder de teste com valores padrão válidos para {@link Vehicle#reconstitute}. */
    private static final class ReconstituteArgs {
        Long id = 1L;
        String nickname;
        String photo;
        String plate;
        String renavam;
        String brand;
        String model;
        int manufacturingYear;
        int modelYear;
        String color;
        int seatingCapacity;
        FuelType fuelType;
        double engineDisplacement;
        VehicleStatus status = VehicleStatus.ACTIVE;
        Boolean towing;
        Long ownerId;
        Instant createdAt;
        Instant updatedAt = null;

        ReconstituteArgs(CreateArgs base) {
            this.nickname = base.nickname;
            this.photo = base.photo;
            this.plate = base.plate;
            this.renavam = base.renavam;
            this.brand = base.brand;
            this.model = base.model;
            this.manufacturingYear = base.manufacturingYear;
            this.modelYear = base.modelYear;
            this.color = base.color;
            this.seatingCapacity = base.seatingCapacity;
            this.fuelType = base.fuelType;
            this.engineDisplacement = base.engineDisplacement;
            this.towing = base.towing;
            this.ownerId = base.ownerId;
            this.createdAt = base.createdAt;
        }

        ReconstituteArgs id(Long value) { this.id = value; return this; }
        ReconstituteArgs renavam(String value) { this.renavam = value; return this; }
        ReconstituteArgs ownerId(Long value) { this.ownerId = value; return this; }
        ReconstituteArgs brand(String value) { this.brand = value; return this; }
        ReconstituteArgs manufacturingYear(int value) { this.manufacturingYear = value; return this; }
        ReconstituteArgs seatingCapacity(int value) { this.seatingCapacity = value; return this; }
        ReconstituteArgs engineDisplacement(double value) { this.engineDisplacement = value; return this; }
        ReconstituteArgs fuelType(FuelType value) { this.fuelType = value; return this; }
        ReconstituteArgs status(VehicleStatus value) { this.status = value; return this; }
        ReconstituteArgs towing(Boolean value) { this.towing = value; return this; }
        ReconstituteArgs createdAt(Instant value) { this.createdAt = value; return this; }
        ReconstituteArgs updatedAt(Instant value) { this.updatedAt = value; return this; }

        Vehicle build() {
            return Vehicle.reconstitute(
                    id, nickname, photo, plate, renavam, brand, model, manufacturingYear,
                    modelYear, color, seatingCapacity, fuelType, engineDisplacement,
                    status, towing, ownerId, createdAt, updatedAt
            );
        }
    }
}
