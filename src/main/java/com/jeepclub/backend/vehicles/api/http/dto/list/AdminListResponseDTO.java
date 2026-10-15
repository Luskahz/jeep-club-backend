package com.jeepclub.backend.vehicles.api.http.dto.list;

import com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Veículo com identificação do proprietário para listagem administrativa.")
public record AdminListResponseDTO(Long id, String nickname, String plate, String photo,
        int modelYear, String model, String color, Long ownerId, String ownerName) {
    public static AdminListResponseDTO from(AdminVehicleSummary summary) {
        var vehicle = summary.vehicle();
        return new AdminListResponseDTO(vehicle.getId(), vehicle.getNickname(), vehicle.getPlate(),
                vehicle.getPhoto(), vehicle.getModelYear(), vehicle.getModel(), vehicle.getColor(),
                vehicle.getOwnerId(), summary.ownerName());
    }
}
