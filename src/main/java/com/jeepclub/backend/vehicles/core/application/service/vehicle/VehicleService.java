package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.platform.storage.image.ImageMediaCommand;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.service.internal.VehicleEditResolver;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final Clock clock;
    private final ImageMediaService images;
    private final ImageMediaCommand mediaCommand;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;
    private final java.util.List<com.jeepclub.backend.vehicles.api.module.spi.VehicleDeletionGuard> deletionGuards;

    @Transactional
    public Vehicle create(
            String nickname,
            ImageUpload photoImage,
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
            Boolean towing,
            Long ownerId
    ) {
        String canonicalPlate = Vehicle.normalizePlate(plate);
        String canonicalRenavam = Vehicle.normalizeRenavam(renavam);
        assertUniquePlateAndRenavam(canonicalPlate, canonicalRenavam);
        String photo = photoImage == null ? null : mediaCommand.storeInTransaction(photoImage);

        Vehicle vehicle = Vehicle.create(
                nickname,
                photo,
                canonicalPlate,
                canonicalRenavam,
                brand,
                model,
                manufacturingYear,
                modelYear,
                color,
                seatingCapacity,
                fuelType,
                engineDisplacement,
                towing,
                ownerId,
                Instant.now(clock)
        );

        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<Vehicle> findAll(Long ownerId, Pageable pageable) {
        return vehicleRepository.findAllByOwnerIdAndStatus(
                ownerId,
                VehicleStatus.ACTIVE,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long vehicleId, Long ownerId) {
        var vehicle=vehicleRepository.findByIdAndOwnerId(vehicleId,ownerId).orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));
        if(vehicle.isLegacyDeleted()) throw new VehicleIdNotFoundException("Vehicle not found.");
        return vehicle;
    }

    @Transactional
    public void update(Long vehicleId, Long ownerId, VehicleEditFields updates) {
        Vehicle vehicle = getForUpdate(vehicleId,ownerId);
        if(vehicle.getStatus()!=VehicleStatus.ACTIVE) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Only active vehicles can be edited.");
        if (updates.photo().isPresentValue()) {
            images.requireExisting(updates.photo().value());
        }
        VehicleEditResolver.Resolved resolved = VehicleEditResolver.resolve(vehicle, updates);
        String canonicalPlate = Vehicle.normalizePlate(resolved.plate());
        String canonicalRenavam = Vehicle.normalizeRenavam(resolved.renavam());
        assertUniqueChangedIdentifiers(vehicle, canonicalPlate, canonicalRenavam);

        vehicle.update(
                resolved.nickname(),
                resolved.photo(),
                canonicalPlate,
                canonicalRenavam,
                resolved.brand(),
                resolved.model(),
                resolved.manufacturingYear(),
                resolved.modelYear(),
                resolved.color(),
                resolved.seatingCapacity(),
                resolved.fuelType(),
                resolved.engineDisplacement(),
                resolved.towing(),
                Instant.now(clock)
        );

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void delete(Long vehicleId, Long ownerId) {
        Vehicle vehicle = getForUpdate(vehicleId,ownerId);
        if(deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(vehicleId)))
            throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Vehicle has retained Event references.");
        vehicleRepository.delete(
                vehicle,
                ownerId,
                Instant.now(clock)
        );
    }

    private Vehicle findActiveVehicle(Long vehicleId, Long ownerId) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        assertActive(vehicle);
        return vehicle;
    }

    private void assertUniquePlateAndRenavam(String plate, String renavam) {
        if (vehicleRepository.existsByPlate(plate)) {
            throw new VehiclePlateAlreadyExistsException(
                    "The license PLATE provided is already registered."
            );
        }

        if (vehicleRepository.existsByRenavam(renavam)) {
            throw new VehicleRenavamAlreadyExistsException(
                    "The RENAVAM number provided is already registered."
            );
        }
    }

    private void assertUniqueChangedIdentifiers(
            Vehicle vehicle,
            String plate,
            String renavam
    ) {
        if (!vehicle.getPlate().equals(plate) && vehicleRepository.existsByPlate(plate)) {
            throw new VehiclePlateAlreadyExistsException(
                    "The license PLATE provided is already registered."
            );
        }

        if (!vehicle.getRenavam().equals(renavam)
                && vehicleRepository.existsByRenavam(renavam)) {
            throw new VehicleRenavamAlreadyExistsException(
                    "The RENAVAM number provided is already registered."
            );
        }
    }

    private void assertActive(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }
    }

    @Transactional
    public Vehicle activate(Long id, Long ownerId) {
        var vehicle=getForUpdate(id,ownerId);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.activate(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(ownerId,
                    "VEHICLE_ACTIVATE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/activate/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle deactivate(Long id, Long ownerId) {
        var vehicle=getForUpdate(id,ownerId);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.deactivate(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(ownerId,
                    "VEHICLE_DEACTIVATE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/deactivate/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle archive(Long id, Long ownerId) {
        var vehicle=getForUpdate(id,ownerId);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.archive(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(ownerId,
                    "VEHICLE_ARCHIVE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/archive/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle unarchive(Long id, Long ownerId) {
        var vehicle=getForUpdate(id,ownerId);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.unarchive(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(ownerId,
                    "VEHICLE_UNARCHIVE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/unarchive/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    private Vehicle getForUpdate(Long id,Long ownerId) {
        var vehicle=vehicleRepository.findByIdForUpdate(id).orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));
        if(!vehicle.getOwnerId().equals(ownerId)) throw new VehicleIdNotFoundException("Vehicle not found.");
        return vehicle;
    }

    @Transactional(readOnly=true)
    public Page<Vehicle> findAll(Long ownerId,VehicleStatus status,Pageable pageable) {
        return vehicleRepository.findAllByOwnerIdAndStatus(ownerId,status,pageable);
    }
}
