package com.jeepclub.backend.vehicles.infra.persistence.mapper;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;

/**
 * Utilitário responsável pela conversão bidirecional entre Camada de Infra (Entity)
 * e Camada Core (Domain Model). Garante que a entidade do banco nunca suba para a regra
 * de negócio e vice-versa.
 */
public class VehicleMapper {

    private VehicleMapper() {

    }

    /**
     * Converte de JPA Entity para Modelo de Domínio
     * Utiliza o método estático 'reconstitute' do Core para manter a integridade sem alterar regras
     */
    public static Vehicle toDomain(VehicleEntity entity) {
        if (entity == null) {
            return null;
        }

        return Vehicle.reconstitute(
                entity.getId(),
                entity.getNickname(),
                entity.getPhoto(),
                entity.getPlate(),
                entity.getRenavam(),
                entity.getBrand(),
                entity.getModel(),
                entity.getManufacturingYear(),
                entity.getModelYear(),
                entity.getColor(),
                entity.getSeatingCapacity(),
                entity.getFuelType(),
                entity.getEngineDisplacement(),
                entity.getStatus(),
                entity.getTowing(),
                entity.getOwnerId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDisabledAt()
        );
    }

    /**
     * Converte de Modelo de Domínio para JPA Entity
     */
    public static VehicleEntity toEntity(Vehicle domain) {
        if (domain == null) {
            return null;
        }

        VehicleEntity entity = new VehicleEntity();
        entity.setId(domain.getId());
        entity.setNickname(domain.getNickname());
        entity.setPhoto(domain.getPhoto());
        entity.setPlate(domain.getPlate());
        entity.setRenavam(domain.getRenavam());
        entity.setBrand(domain.getBrand());
        entity.setModel(domain.getModel());
        entity.setManufacturingYear(domain.getManufacturingYear());
        entity.setModelYear(domain.getModelYear());
        entity.setColor(domain.getColor());
        entity.setSeatingCapacity(domain.getSeatingCapacity());
        entity.setFuelType(domain.getFuelType());
        entity.setEngineDisplacement(domain.getEngineDisplacement());
        entity.setStatus(domain.getStatus());
        entity.setTowing(domain.getTowing());
        entity.setOwnerId(domain.getOwnerId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDisabledAt(domain.getDeletedAt());

        return entity;
    }
}