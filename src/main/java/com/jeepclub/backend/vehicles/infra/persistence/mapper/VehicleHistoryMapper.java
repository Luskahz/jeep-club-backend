package com.jeepclub.backend.vehicles.infra.persistence.mapper;

import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class VehicleHistoryMapper {

    public VehicleHistoryEntity toHistoryEntity(
            VehicleEntity source,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        if (source == null) {
            return null;
        }

        VehicleHistoryEntity history = new VehicleHistoryEntity();
        history.setVehicleId(source.getId());
        history.setNickname(source.getNickname());
        history.setPhoto(source.getPhoto());
        history.setPlate(source.getPlate());
        history.setRenavam(source.getRenavam());
        history.setBrand(source.getBrand());
        history.setModel(source.getModel());
        history.setManufacturingYear(source.getManufacturingYear());
        history.setModelYear(source.getModelYear());
        history.setColor(source.getColor());
        history.setSeatingCapacity(source.getSeatingCapacity());
        history.setFuelType(source.getFuelType());
        history.setEngineDisplacement(source.getEngineDisplacement());
        history.setStatus(source.getStatus());
        history.setTowing(source.getTowing());
        history.setOwnerId(source.getOwnerId());
        history.setDeletedByUserId(deletedByUserId);
        history.setCreatedAt(source.getCreatedAt());
        history.setUpdatedAt(source.getUpdatedAt());
        history.setDisabledAt(source.getDisabledAt());
        history.setDeletedAt(deletedAt);
        return history;
    }
}
