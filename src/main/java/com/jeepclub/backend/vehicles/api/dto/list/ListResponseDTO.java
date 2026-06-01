package com.jeepclub.backend.vehicles.api.dto.list;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;

public record ListResponseDTO(
        Long id,
        String nickname,
        String plate,
        String photo,
        int modelYear,
        String model,
        String color
) {
    public static ListResponseDTO from(Vehicle vehicle) {
        return new ListResponseDTO(
                vehicle.getId(),
                vehicle.getNickname(),
                vehicle.getPlate(),
                vehicle.getPhoto(),
                vehicle.getModelYear(),
                vehicle.getModel(),
                vehicle.getColor()
        );
    }
}