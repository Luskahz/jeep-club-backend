package com.jeepclub.backend.vehicles.core.application.result;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;

public record AdminVehicleSearchResult(Long id, String nickname, String plate, String photo, Integer modelYear,
        String model, String color, Long ownerId, String ownerName, VehicleStatus status) {
    public AdminVehicleSearchResult withOwnerName(String name) {return new AdminVehicleSearchResult(id,nickname,plate,photo,modelYear,model,color,ownerId,name,status);}
}
