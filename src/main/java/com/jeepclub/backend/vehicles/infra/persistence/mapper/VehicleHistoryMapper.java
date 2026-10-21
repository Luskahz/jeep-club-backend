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
        if(source==null) return null;
        return toHistoryEntity(source,deletedByUserId,deletedAt,VehicleMapper.toDomain(source).getGallery(),source.getGalleryRevision());
    }

    public VehicleHistoryEntity toHistoryEntity(VehicleEntity source,Long deletedByUserId,Instant deletedAt,
            com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery gallery,long revision) {
        if (source == null) {
            return null;
        }

        VehicleHistoryEntity history = new VehicleHistoryEntity();
        history.setVehicleId(source.getId());
        history.setNickname(source.getNickname());
        history.setPhoto(gallery.primaryReference());
        history.setGalleryRevision(revision);
        history.setImages(gallery.images().stream().map(i -> new com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleImageEntity(i.storageKey(),i.position(),i.primary())).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
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
        history.setArchivedAt(source.getArchivedAt());
        history.setLegacyDeleted(source.isLegacyDeleted());
        history.setDeletedAt(deletedAt);
        return history;
    }
}
