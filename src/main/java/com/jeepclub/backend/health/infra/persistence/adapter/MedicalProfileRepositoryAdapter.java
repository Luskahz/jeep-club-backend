package com.jeepclub.backend.health.infra.persistence.adapter;

import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileConflictException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceUnavailableException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.MedicalProfileAlreadyDeletedException;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileHistoryEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileHistoryMapper;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.exception.ConstraintViolationException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
public class MedicalProfileRepositoryAdapter implements MedicalProfileRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            MedicalProfileRepositoryAdapter.class
    );

    private final MedicalProfileJpaRepository medicalProfileJpaRepository;
    private final MedicalProfileHistoryJpaRepository historyJpaRepository;
    private final MedicalProfileMapper medicalProfileMapper;
    private final MedicalProfileHistoryMapper historyMapper;

    @Override
    public MedicalProfile save(MedicalProfile medicalProfile) {
        try {
            var entity = medicalProfileMapper.toEntity(medicalProfile);
            var saved = medicalProfileJpaRepository.saveAndFlush(entity);

            return medicalProfileMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isOwnerUniqueConstraintViolation(exception)) {
                observe(
                        "save",
                        "conflict",
                        "owner_unique",
                        medicalProfile.getOwnerType(),
                        medicalProfile.getOwnerId()
                );
                throw MedicalProfileConflictException.ownerAlreadyHasProfile(
                        medicalProfile.getOwnerType(),
                        medicalProfile.getOwnerId(),
                        exception
                );
            }
            throw persistenceFailure(
                    "save",
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId(),
                    exception
            );
        } catch (PessimisticLockingFailureException exception) {
            observe(
                    "save",
                    "conflict",
                    "lock",
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId()
            );
            throw MedicalProfileConflictException.concurrentUpdate(
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId(),
                    exception
            );
        } catch (PessimisticLockException
                 | LockTimeoutException
                 | OptimisticLockException exception) {
            observe(
                    "save",
                    "conflict",
                    "lock",
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId()
            );
            throw MedicalProfileConflictException.concurrentUpdate(
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId(),
                    exception
            );
        } catch (TransientDataAccessException exception) {
            throw persistenceUnavailable(
                    "save",
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId(),
                    exception
            );
        } catch (DataAccessException | PersistenceException exception) {
            throw persistenceFailure(
                    "save",
                    medicalProfile.getOwnerType(),
                    medicalProfile.getOwnerId(),
                    exception
            );
        }
    }

    @Override
    public Optional<MedicalProfile> findById(Long id) {
        return execute(
                "find_by_id",
                null,
                id,
                () -> medicalProfileJpaRepository
                        .findById(id)
                        .map(medicalProfileMapper::toDomain)
        );
    }

    @Override
    public Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return execute(
                "find_by_owner",
                ownerType,
                ownerId,
                () -> medicalProfileJpaRepository
                        .findByOwnerTypeAndOwnerId(ownerType, ownerId)
                        .map(medicalProfileMapper::toDomain)
        );
    }

    @Override
    public Optional<MedicalProfile> findByOwnerForUpdate(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return execute(
                "find_by_owner_for_update",
                ownerType,
                ownerId,
                () -> medicalProfileJpaRepository
                        .findByOwnerForUpdate(ownerType, ownerId)
                        .map(medicalProfileMapper::toDomain)
        );
    }

    @Override
    public boolean existsByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return execute(
                "exists_by_owner",
                ownerType,
                ownerId,
                () -> medicalProfileJpaRepository
                        .existsByOwnerTypeAndOwnerId(ownerType, ownerId)
        );
    }

    @Override
    public List<MedicalProfile> findAll(
            int page,
            int size
    ) {
        return execute(
                "find_all",
                null,
                null,
                () -> medicalProfileJpaRepository
                        .findAll(PageRequest.of(page, size))
                        .stream()
                        .map(medicalProfileMapper::toDomain)
                        .toList()
        );
    }

    @Override
    public void delete(
            MedicalProfile medicalProfile,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        execute(
                "delete",
                medicalProfile.getOwnerType(),
                medicalProfile.getOwnerId(),
                () -> {
                    MedicalProfileEntity entity = medicalProfileJpaRepository
                            .findByIdForUpdate(medicalProfile.getId())
                            .orElseThrow(
                                    () -> new MedicalProfileAlreadyDeletedException(
                                            medicalProfile.getId()
                                    )
                            );

                    MedicalProfileHistoryEntity history = historyMapper
                            .toHistoryEntity(entity, deletedByUserId, deletedAt);
                    historyJpaRepository.saveAndFlush(history);
                    medicalProfileJpaRepository.delete(entity);
                    medicalProfileJpaRepository.flush();
                    return null;
                }
        );
    }

    private <T> T execute(
            String operation,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Supplier<T> action
    ) {
        try {
            return action.get();
        } catch (PessimisticLockingFailureException exception) {
            observe(operation, "conflict", "lock", ownerType, ownerId);
            throw MedicalProfileConflictException.concurrentUpdate(
                    ownerType,
                    ownerId,
                    exception
            );
        } catch (PessimisticLockException
                 | LockTimeoutException
                 | OptimisticLockException exception) {
            observe(operation, "conflict", "lock", ownerType, ownerId);
            throw MedicalProfileConflictException.concurrentUpdate(
                    ownerType,
                    ownerId,
                    exception
            );
        } catch (TransientDataAccessException exception) {
            throw persistenceUnavailable(operation, ownerType, ownerId, exception);
        } catch (DataAccessException | PersistenceException exception) {
            throw persistenceFailure(operation, ownerType, ownerId, exception);
        }
    }

    private MedicalProfilePersistenceUnavailableException persistenceUnavailable(
            String operation,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Throwable cause
    ) {
        observe(operation, "failed", "transient", ownerType, ownerId);
        return new MedicalProfilePersistenceUnavailableException(cause);
    }

    private MedicalProfilePersistenceException persistenceFailure(
            String operation,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Throwable cause
    ) {
        observe(operation, "failed", "persistence", ownerType, ownerId);
        return new MedicalProfilePersistenceException(cause);
    }

    private void observe(
            String operation,
            String outcome,
            String reason,
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        LOGGER.warn(
                "Health persistence event operation={} outcome={} reason={} ownerType={} ownerId={}",
                operation,
                outcome,
                reason,
                ownerType,
                ownerId
        );
    }

    private boolean isOwnerUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                return constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT)
                        .contains("uk_medical_profile_owner");
            }
            cause = cause.getCause();
        }

        return false;
    }
}
