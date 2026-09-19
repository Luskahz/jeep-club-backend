package com.jeepclub.backend.vehicles.core.application;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;

/**
 * Feixe de atualizações por campo para a edição de um veículo. Cada campo
 * carrega seu próprio {@link FieldUpdate}, permitindo que ausência, null
 * explícito e valor sejam tratados de forma independente por campo.
 */
public record VehicleEditFields(
        FieldUpdate<String> nickname,
        FieldUpdate<String> photo,
        FieldUpdate<String> plate,
        FieldUpdate<String> renavam,
        FieldUpdate<String> brand,
        FieldUpdate<String> model,
        FieldUpdate<Integer> manufacturingYear,
        FieldUpdate<Integer> modelYear,
        FieldUpdate<String> color,
        FieldUpdate<Integer> seatingCapacity,
        FieldUpdate<FuelType> fuelType,
        FieldUpdate<Double> engineDisplacement,
        FieldUpdate<Boolean> towing
) {
}
