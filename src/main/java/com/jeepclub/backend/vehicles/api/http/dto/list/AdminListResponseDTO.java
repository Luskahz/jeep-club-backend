package com.jeepclub.backend.vehicles.api.http.dto.list;

import com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Veículo com identificação do proprietário para listagem administrativa.")
public record AdminListResponseDTO(Long id, String nickname, String plate, @Schema(deprecated=true,description="Alias de capa da imagem principal") String photo,
        int modelYear, String model, String color, Long ownerId, String ownerName, com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus status) {
    public static AdminListResponseDTO from(com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSearchResult row) {
        return new AdminListResponseDTO(row.id(),row.nickname(),row.plate(),row.photo(),row.modelYear(),row.model(),row.color(),row.ownerId(),row.ownerName(),row.status());
    }
    public static java.util.Map<String,Object> selected(com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSearchResult row,
            java.util.Set<com.jeepclub.backend.vehicles.core.application.query.AdminVehicleField> fields) {
        var result=new java.util.LinkedHashMap<String,Object>();
        for(var field:fields) result.put(field.property(),switch(field) {
            case ID -> row.id(); case NICKNAME -> row.nickname(); case PLATE -> row.plate(); case PHOTO -> row.photo();
            case MODEL_YEAR -> row.modelYear(); case MODEL -> row.model(); case COLOR -> row.color();
            case OWNER_ID -> row.ownerId(); case OWNER_NAME -> row.ownerName(); case STATUS -> row.status();
        });
        return result;
    }
    public static AdminListResponseDTO from(AdminVehicleSummary summary) {
        var vehicle = summary.vehicle();
        return new AdminListResponseDTO(vehicle.getId(), vehicle.getNickname(), vehicle.getPlate(),
                vehicle.getPhoto(), vehicle.getModelYear(), vehicle.getModel(), vehicle.getColor(),
                vehicle.getOwnerId(), summary.ownerName(), vehicle.getStatus());
    }
}
