package com.jeepclub.backend.health.infra.persistence.adapter;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicalProfileRepositoryAdapter implements MedicalProfileRepository {

    private final MedicalProfileJpaRepository medicalProfileJpaRepository;
    private final MedicalProfileMapper medicalProfileMapper;

    @Override
    public MedicalProfile save(MedicalProfile medicalProfile) {
        var entity = medicalProfileMapper.toEntity(medicalProfile);
        var saved = medicalProfileJpaRepository.save(entity);

        return medicalProfileMapper.toDomain(saved);
    }

    @Override
    public Optional<MedicalProfile> findById(Long id) {
        return medicalProfileJpaRepository
                .findById(id)
                .map(medicalProfileMapper::toDomain);
    }

    @Override
    public Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileJpaRepository
                .findByOwnerTypeAndOwnerId(
                        ownerType,
                        ownerId
                )
                .map(medicalProfileMapper::toDomain);
    }

    @Override
    public boolean existsByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileJpaRepository
                .existsByOwnerTypeAndOwnerId(
                        ownerType,
                        ownerId
                );
    }

    @Override
    public List<MedicalProfile> findAll(
            int page,
            int size
    ) {
        return medicalProfileJpaRepository
                .findAll(PageRequest.of(page, size))
                .stream()
                .map(medicalProfileMapper::toDomain)
                .toList();
    }
}