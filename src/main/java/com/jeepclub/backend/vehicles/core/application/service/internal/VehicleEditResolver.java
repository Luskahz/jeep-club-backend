package com.jeepclub.backend.vehicles.core.application.service.internal;

import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleFieldRequiredException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;

/**
 * Resolve um {@link VehicleEditFields} contra o estado atual de um
 * {@link Vehicle}: campo ausente preserva o valor atual, campo com valor
 * aplica o novo valor, e campo null explícito só é aceito quando o campo é
 * realmente anulável no domínio. Compartilhado pelas edições de membro e
 * administrador para manter a mesma semântica nos dois fluxos.
 */
public final class VehicleEditResolver {

    private VehicleEditResolver() {
    }

    public static Resolved resolve(Vehicle current, VehicleEditFields updates) {
        return new Resolved(
                resolveNullable(updates.nickname(), current.getNickname()),
                resolveNullable(updates.photo(), current.getPhoto()),
                resolveRequired(updates.plate(), current.getPlate(), "plate"),
                resolveRequired(updates.renavam(), current.getRenavam(), "renavam"),
                resolveRequired(updates.brand(), current.getBrand(), "brand"),
                resolveRequired(updates.model(), current.getModel(), "model"),
                resolveRequired(updates.manufacturingYear(), current.getManufacturingYear(), "manufacturingYear"),
                resolveRequired(updates.modelYear(), current.getModelYear(), "modelYear"),
                resolveNullable(updates.color(), current.getColor()),
                resolveRequired(updates.seatingCapacity(), current.getSeatingCapacity(), "seatingCapacity"),
                resolveRequired(updates.fuelType(), current.getFuelType(), "fuelType"),
                resolveRequired(updates.engineDisplacement(), current.getEngineDisplacement(), "engineDisplacement"),
                resolveRequired(updates.towing(), current.getTowing(), "towing")
        );
    }

    private static <T> T resolveNullable(FieldUpdate<T> update, T currentValue) {
        if (update.isOmitted()) {
            return currentValue;
        }
        if (update.isExplicitNull()) {
            return null;
        }
        return update.value();
    }

    private static <T> T resolveRequired(FieldUpdate<T> update, T currentValue, String fieldName) {
        if (update.isOmitted()) {
            return currentValue;
        }
        if (update.isExplicitNull()) {
            throw new VehicleFieldRequiredException(fieldName + " cannot be cleared to null.");
        }
        return update.value();
    }

    public record Resolved(
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            Boolean towing
    ) {
    }
}
