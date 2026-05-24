package com.jeepclub.backend.medical.infra.persistence.repository;

import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.medical.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.medical.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.medical.infra.persistence.mapper.MedicalProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicalProfileRepositoryJpa implements MedicalProfileRepository {

    private final MedicalProfileJpaRepository medicalProfileJpaRepository;
    private final MedicalProfileMapper medicalProfileMapper;

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
                .findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(medicalProfileMapper::toDomain);
    }

    @Override
    public List<MedicalProfile> findAll(int page, int size) {
        return medicalProfileJpaRepository
                .findAll(PageRequest.of(page, size))
                .stream()
                .map(medicalProfileMapper::toDomain)
                .toList();
    }

    @Override
    public MedicalProfile save(MedicalProfile medicalProfile) {
        var entity = medicalProfileMapper.toEntity(medicalProfile);
        var saved = medicalProfileJpaRepository.save(entity);
        return medicalProfileMapper.toDomain(saved);
    }
}
