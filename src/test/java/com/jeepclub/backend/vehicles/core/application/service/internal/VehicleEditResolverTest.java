package com.jeepclub.backend.vehicles.core.application.service.internal;

import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleFieldRequiredException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleEditResolverTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void omittedFieldsPreserveCurrentValuesForEveryField() {
        Vehicle current = currentVehicle();

        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(current, allOmitted());

        assertThat(resolved.nickname()).isEqualTo(current.getNickname());
        assertThat(resolved.photo()).isEqualTo(current.getPhoto());
        assertThat(resolved.plate()).isEqualTo(current.getPlate());
        assertThat(resolved.renavam()).isEqualTo(current.getRenavam());
        assertThat(resolved.brand()).isEqualTo(current.getBrand());
        assertThat(resolved.model()).isEqualTo(current.getModel());
        assertThat(resolved.manufacturingYear()).isEqualTo(current.getManufacturingYear());
        assertThat(resolved.modelYear()).isEqualTo(current.getModelYear());
        assertThat(resolved.color()).isEqualTo(current.getColor());
        assertThat(resolved.seatingCapacity()).isEqualTo(current.getSeatingCapacity());
        assertThat(resolved.fuelType()).isEqualTo(current.getFuelType());
        assertThat(resolved.engineDisplacement()).isEqualTo(current.getEngineDisplacement());
        assertThat(resolved.towing()).isEqualTo(current.getTowing());
    }

    @Test
    void presentValuesOverrideCurrentValues() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .nickname(FieldUpdate.of("Novo apelido"))
                .seatingCapacity(FieldUpdate.of(7))
                .build();

        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(current, updates);

        assertThat(resolved.nickname()).isEqualTo("Novo apelido");
        assertThat(resolved.seatingCapacity()).isEqualTo(7);
        assertThat(resolved.model()).isEqualTo(current.getModel());
    }

    @Test
    void explicitFalseIsAppliedAndNotConfusedWithOmission() {
        Vehicle current = currentVehicle();
        assertThat(current.getTowing()).isEqualTo(true);

        VehicleEditFields updates = allOmittedBuilder()
                .towing(FieldUpdate.of(false))
                .build();

        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(current, updates);

        assertThat(resolved.towing()).isEqualTo(false);
    }

    @Test
    void explicitZeroIsAppliedAndNotConfusedWithOmission() {
        Vehicle current = currentVehicle();
        assertThat(current.getEngineDisplacement()).isNotEqualTo(0.0);

        VehicleEditFields updates = allOmittedBuilder()
                .engineDisplacement(FieldUpdate.of(0.0))
                .build();

        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(current, updates);

        assertThat(resolved.engineDisplacement()).isEqualTo(0.0);
    }

    @Test
    void explicitNullClearsOnlyNullableFields() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .nickname(FieldUpdate.explicitNull())
                .photo(FieldUpdate.explicitNull())
                .color(FieldUpdate.explicitNull())
                .build();

        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(current, updates);

        assertThat(resolved.nickname()).isNull();
        assertThat(resolved.photo()).isNull();
        assertThat(resolved.color()).isNull();
        assertThat(resolved.towing()).isEqualTo(current.getTowing());
    }

    @Test
    void explicitNullOnTowingIsRejectedBecauseItIsRequired() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .towing(FieldUpdate.explicitNull())
                .build();

        assertThatThrownBy(() -> VehicleEditResolver.resolve(current, updates))
                .isInstanceOf(VehicleFieldRequiredException.class)
                .hasMessage("towing cannot be cleared to null.");
    }

    @Test
    void explicitNullOnRequiredStringFieldIsRejected() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .plate(FieldUpdate.explicitNull())
                .build();

        assertThatThrownBy(() -> VehicleEditResolver.resolve(current, updates))
                .isInstanceOf(VehicleFieldRequiredException.class);
    }

    @Test
    void explicitNullOnRequiredPrimitiveFieldIsRejected() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .seatingCapacity(FieldUpdate.explicitNull())
                .build();

        assertThatThrownBy(() -> VehicleEditResolver.resolve(current, updates))
                .isInstanceOf(VehicleFieldRequiredException.class);
    }

    @Test
    void explicitNullOnRequiredEnumFieldIsRejected() {
        Vehicle current = currentVehicle();
        VehicleEditFields updates = allOmittedBuilder()
                .fuelType(FieldUpdate.explicitNull())
                .build();

        assertThatThrownBy(() -> VehicleEditResolver.resolve(current, updates))
                .isInstanceOf(VehicleFieldRequiredException.class);
    }

    private Vehicle currentVehicle() {
        return Vehicle.reconstitute(
                1L, "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, VehicleStatus.ACTIVE, true, 7L, CREATED_AT, null, null
        );
    }

    private VehicleEditFields allOmitted() {
        return allOmittedBuilder().build();
    }

    private Builder allOmittedBuilder() {
        return new Builder();
    }

    /** Pequeno builder de teste para deixar explícito só o campo sob teste em cada cenário. */
    private static final class Builder {
        private FieldUpdate<String> nickname = FieldUpdate.omitted();
        private FieldUpdate<String> photo = FieldUpdate.omitted();
        private FieldUpdate<String> plate = FieldUpdate.omitted();
        private FieldUpdate<String> renavam = FieldUpdate.omitted();
        private FieldUpdate<String> brand = FieldUpdate.omitted();
        private FieldUpdate<String> model = FieldUpdate.omitted();
        private FieldUpdate<Integer> manufacturingYear = FieldUpdate.omitted();
        private FieldUpdate<Integer> modelYear = FieldUpdate.omitted();
        private FieldUpdate<String> color = FieldUpdate.omitted();
        private FieldUpdate<Integer> seatingCapacity = FieldUpdate.omitted();
        private FieldUpdate<FuelType> fuelType = FieldUpdate.omitted();
        private FieldUpdate<Double> engineDisplacement = FieldUpdate.omitted();
        private FieldUpdate<Boolean> towing = FieldUpdate.omitted();

        Builder nickname(FieldUpdate<String> value) { this.nickname = value; return this; }
        Builder photo(FieldUpdate<String> value) { this.photo = value; return this; }
        Builder plate(FieldUpdate<String> value) { this.plate = value; return this; }
        Builder seatingCapacity(FieldUpdate<Integer> value) { this.seatingCapacity = value; return this; }
        Builder fuelType(FieldUpdate<FuelType> value) { this.fuelType = value; return this; }
        Builder engineDisplacement(FieldUpdate<Double> value) { this.engineDisplacement = value; return this; }
        Builder color(FieldUpdate<String> value) { this.color = value; return this; }
        Builder towing(FieldUpdate<Boolean> value) { this.towing = value; return this; }

        VehicleEditFields build() {
            return new VehicleEditFields(
                    nickname, photo, plate, renavam, brand, model, manufacturingYear,
                    modelYear, color, seatingCapacity, fuelType, engineDisplacement, towing
            );
        }
    }
}
