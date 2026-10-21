package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.platform.storage.image.ImageMediaCommand;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotActiveException;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.service.internal.VehicleEditResolver;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.port.UserPort;
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
public class AdminVehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserPort userPort;
    private final Clock clock;
    private final ImageMediaService images;
    private final ImageMediaCommand mediaCommand;
    private final com.jeepclub.backend.vehicles.core.application.service.internal.VehicleGalleryAssembler galleryAssembler;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;
    private final java.util.List<com.jeepclub.backend.vehicles.api.module.spi.VehicleDeletionGuard> deletionGuards;

    @Transactional
    public Vehicle createForOwner(
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

        if (!userPort.existsById(ownerId)) {
            throw new UserNotFoundException("User id not found.");
        }

        if (!userPort.existsActiveById(ownerId)) {
            throw new UserNotActiveException("User is not administratively active.");
        }

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

    @Transactional
    public Vehicle createWithGallery(
            String nickname,
            com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange change,
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
            Long ownerId,
            Long actorId
    ) {
        String canonicalPlate = Vehicle.normalizePlate(plate);
        String canonicalRenavam = Vehicle.normalizeRenavam(renavam);
        assertUniquePlateAndRenavam(canonicalPlate, canonicalRenavam);

        if (!userPort.existsById(ownerId)) {
            throw new UserNotFoundException("User id not found.");
        }

        if (!userPort.existsActiveById(ownerId)) {
            throw new UserNotActiveException("User is not administratively active.");
        }

        if(change.expectedRevision()!=null && change.expectedRevision()!=0L) throw new IllegalArgumentException("Creation has no prior gallery revision.");

        Vehicle vehicle = Vehicle.create(
                nickname,
                null,
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

        vehicle.initializeGallery(galleryAssembler.assemble(com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery.empty(),change));
        var saved=vehicleRepository.save(vehicle);
        if(!saved.getImages().isEmpty()) recordGallery(actorId,saved.getId(),"CREATE",201);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Vehicle> findAll(Pageable pageable) {
        return vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary> findAllWithOwners(Pageable pageable) {
        Page<Vehicle> vehicles = findAll(pageable);
        var names = userPort.findNamesByIds(vehicles.stream().map(Vehicle::getOwnerId).distinct().toList());
        return vehicles.map(vehicle -> new com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary(
                vehicle, names.get(vehicle.getOwnerId())));
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long vehicleId) {
        var vehicle=vehicleRepository.findById(vehicleId).orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));
        if(vehicle.isLegacyDeleted()) throw new VehicleIdNotFoundException("Vehicle not found.");
        return vehicle;
    }

    @Transactional
    public void update(Long vehicleId, VehicleEditFields updates) {
        Vehicle vehicle = getForUpdate(vehicleId);
        if(vehicle.getStatus()!=VehicleStatus.ACTIVE) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Only active vehicles can be edited.");
        if(!updates.photo().isOmitted()) vehicle.assertLegacyPhotoChangeAllowed(updates.photo().isExplicitNull()?null:updates.photo().value());
        if (updates.photo().isPresentValue() && !java.util.Objects.equals(updates.photo().value(),vehicle.getPhoto())) {
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
    public void delete(
            Long vehicleId,
            Long deletedByUserId
    ) {
        Vehicle vehicle = getForUpdate(vehicleId);
        if(deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(vehicleId)))
            throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Vehicle has retained Event references.");
        vehicleRepository.delete(
                vehicle,
                deletedByUserId,
                Instant.now(clock)
        );
    }

    private Vehicle findActiveVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
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
    public Vehicle activate(Long id, Long actorId) {
        var vehicle=getForUpdate(id);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.activate(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "VEHICLE_ACTIVATE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/activate/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle deactivate(Long id, Long actorId) {
        var vehicle=getForUpdate(id);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.deactivate(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "VEHICLE_DEACTIVATE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/deactivate/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle archive(Long id, Long actorId) {
        var vehicle=getForUpdate(id);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.archive(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "VEHICLE_ARCHIVE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/archive/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    @Transactional
    public Vehicle unarchive(Long id, Long actorId) {
        var vehicle=getForUpdate(id);
        if(vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Historically deleted vehicle cannot be changed.");
        var before=vehicle.getStatus(); var now=Instant.now(clock);
        if(vehicle.unarchive(now)) {
            vehicle=vehicleRepository.save(vehicle);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "VEHICLE_UNARCHIVE:"+before+":"+vehicle.getStatus(),"PATCH","/vehicles/unarchive/"+id,200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
        }
        return vehicle;
    }

    private Vehicle getForUpdate(Long id) {
        var vehicle=vehicleRepository.findByIdForUpdate(id).orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));
        return vehicle;
    }

    @Transactional(readOnly=true)
    public Page<com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary> findAllWithOwners(VehicleStatus status,Pageable pageable) {
        var vehicles=vehicleRepository.findAllByStatus(status,pageable);
        var names=userPort.findNamesByIds(vehicles.stream().map(Vehicle::getOwnerId).distinct().toList());
        return vehicles.map(vehicle -> new com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSummary(vehicle,names.get(vehicle.getOwnerId())));
    }

    private void assertOwnerActive(Long ownerId) {
        if(!userPort.existsById(ownerId)) throw new com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException("User id not found.");
        if(!userPort.existsActiveById(ownerId)) throw new com.jeepclub.backend.vehicles.core.application.exceptions.UserNotActiveException("User is not administratively active.");
    }

    @Transactional
    public Vehicle replaceGallery(Long id,com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange change,Long actorId) {
        var candidate=findById(id);
        assertOwnerActive(candidate.getOwnerId());
        var vehicle=getForUpdate(id);
        if(vehicle.getStatus()!=VehicleStatus.ACTIVE || vehicle.isLegacyDeleted()) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException("Only active vehicles can change their gallery.");
        if(change.expectedRevision()==null) throw new IllegalArgumentException("expectedRevision is required when replacing a gallery.");
        if(!change.uploads().isEmpty()) vehicle.requireGalleryRevision(change.expectedRevision());
        var gallery=galleryAssembler.assemble(vehicle.getGallery(),change);
        if(vehicle.replaceGallery(gallery,change.expectedRevision(),Instant.now(clock))) {
            vehicle=vehicleRepository.save(vehicle);
            recordGallery(actorId,id,"REPLACE",200);
        }
        return vehicle;
    }

    private void recordGallery(Long actor,Long id,String action,int status) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actor,"VEHICLE_GALLERY_"+action,
                action.equals("CREATE")?"POST":"PUT","/vehicles/gallery/"+id,status,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
    }

    @Transactional
    public com.jeepclub.backend.vehicles.core.domain.model.VehicleGallerySnapshot galleryHistory(Long historyId,Long actorId) {
        var snapshot=vehicleRepository.findGalleryHistory(historyId).orElseThrow(() -> new VehicleIdNotFoundException("Vehicle history not found."));
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"VEHICLE_GALLERY_HISTORY_READ","GET",
                "/vehicles/gallery/history/admin/"+historyId,200,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return snapshot;
    }
}
