package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleAlreadyDeletedException;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleHistoryJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleHistoryMapper;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {
    private final jakarta.persistence.EntityManager entityManager;


    private static final String PLATE_CONSTRAINT = "uk_vehicle_plate";
    private static final String RENAVAM_CONSTRAINT = "uk_vehicle_renavam";

    private final VehicleJpaRepository jpaRepository;
    private final VehicleHistoryJpaRepository historyJpaRepository;
    private final VehicleHistoryMapper historyMapper;
    @Override
    public java.util.List<Vehicle> findAllByIds(java.util.Collection<Long> ids) {
        return ids.isEmpty()?java.util.List.of():jpaRepository.findWithImagesByIds(ids).stream().map(VehicleMapper::toDomain).toList();
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleEntity entity = VehicleMapper.toEntity(vehicle);
        try {
            VehicleEntity saved = jpaRepository.saveAndFlush(entity);
            return VehicleMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (violatesConstraint(exception, PLATE_CONSTRAINT)) {
                throw new VehiclePlateAlreadyExistsException(
                        "The license PLATE provided is already registered.",
                        exception
                );
            }
            if (violatesConstraint(exception, RENAVAM_CONSTRAINT)) {
                throw new VehicleRenavamAlreadyExistsException(
                        "The RENAVAM number provided is already registered.",
                        exception
                );
            }
            throw exception;
        }
    }

    private boolean violatesConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null
                        && name.toLowerCase(Locale.ROOT).contains(constraintName);
            }
            cause = cause.getCause();
        }

        return false;
    }

    @Override
    public void delete(
            Vehicle vehicle,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        var current = findByIdForUpdate(vehicle.getId()).orElseThrow(() -> new VehicleAlreadyDeletedException(vehicle.getId()));
        VehicleEntity entity = jpaRepository
                .findByIdForUpdate(vehicle.getId())
                .orElseThrow(
                        () -> new VehicleAlreadyDeletedException(
                                vehicle.getId()
                        )
                );

        VehicleHistoryEntity history = historyMapper.toHistoryEntity(
                entity,
                deletedByUserId,
                deletedAt,
                current.getGallery(),
                current.getGalleryRevision()
        );

        historyJpaRepository.save(history);
        jpaRepository.delete(entity);
    }

    @Override
    public boolean existsByPlate(String plate) {
        return jpaRepository.existsByPlate(plate);
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return jpaRepository.findById(id)
                .map(VehicleMapper::toDomain);
    }

    @Override
    public boolean existsByRenavam(String renavam) {
        return jpaRepository.existsByRenavam(renavam);
    }

    @Override
    public Optional<Vehicle> findByIdAndOwnerId(Long vehicleId, Long memberId) {
        return jpaRepository.findByIdAndOwnerId(vehicleId, memberId)
                .map(VehicleMapper::toDomain);
    }

    @Override
    public boolean existsById(Long vehicleId) {
        return jpaRepository.existsById(vehicleId);
    }

    @Override
    public Page<Vehicle> findAllByOwnerIdAndStatus(Long ownerId, VehicleStatus status, Pageable pageable) {
        var page=jpaRepository.findAllByOwnerIdAndStatusAndLegacyDeletedFalse(ownerId,status,pageable);
        fetchPageImages(page);
        return page.map(VehicleMapper::toDomain);
    }

    @Override
    public Page<Vehicle> findAllByStatus(VehicleStatus status, Pageable pageable) {
        var page=jpaRepository.findAllByStatusAndLegacyDeletedFalse(status,pageable);
        fetchPageImages(page);
        return page.map(VehicleMapper::toDomain);
    }
    @Override
    public Optional<Vehicle> findByIdForUpdate(Long id) {
        var locked = jpaRepository.findByIdForUpdate(id);
        // Retain the DB lock while discarding any pre-lock ORM snapshot (MySQL REPEATABLE READ).
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpaRepository.findByIdForUpdate(id)).map(entity -> {
            // A plain lazy SELECT could reuse the transaction's pre-lock MySQL RR snapshot.
            @SuppressWarnings("unchecked") java.util.List<Object[]> rows=entityManager.createNativeQuery(
                    "select storage_key,position,is_primary from vehicle_images where vehicle_id=:id order by position for update")
                    .setParameter("id",id).getResultList();
            var gallery=rows.isEmpty()?com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery.legacy(entity.getPhoto()):
                    com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery.of(rows.stream().map(row ->
                            new com.jeepclub.backend.vehicles.core.domain.model.VehicleImage((String)row[0],((Number)row[1]).intValue(),
                                Boolean.TRUE.equals(row[2]) || row[2] instanceof Number n && n.intValue()!=0)).toList());
            return VehicleMapper.toDomain(entity,gallery);
        });
    }

    private void fetchPageImages(Page<VehicleEntity> page) {
        if(!page.isEmpty()) jpaRepository.findWithImagesByIds(page.getContent().stream().map(VehicleEntity::getId).toList());
    }

    @Override public Optional<com.jeepclub.backend.vehicles.core.domain.model.VehicleGallerySnapshot> findGalleryHistory(Long id) {
        return historyJpaRepository.findById(id).map(h -> {
            var gallery=h.getImages().isEmpty()?com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery.legacy(h.getPhoto()):
                    com.jeepclub.backend.vehicles.core.domain.model.VehicleGallery.of(h.getImages().stream().map(i -> new com.jeepclub.backend.vehicles.core.domain.model.VehicleImage(i.getStorageKey(),i.getPosition(),i.isPrimary())).toList());
            return new com.jeepclub.backend.vehicles.core.domain.model.VehicleGallerySnapshot(h.getId(),h.getVehicleId(),h.getOwnerId(),h.getDeletedByUserId(),h.getDeletedAt(),h.getGalleryRevision(),gallery);
        });
    }
}
