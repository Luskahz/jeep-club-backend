package com.jeepclub.backend.identity.infra.persistence.adapter;

import com.jeepclub.backend.identity.core.application.exception.IdentityConflictException;
import com.jeepclub.backend.identity.api.module.IdentityStatus;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import com.jeepclub.backend.identity.infra.persistence.jpa.IdentityJpaRepository;
import com.jeepclub.backend.identity.infra.persistence.mapper.IdentityMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdentityRepositoryAdapter implements IdentityRepository {

    private final IdentityJpaRepository jpaRepository;
    private final IdentityMapper mapper;

    @Override
    public Identity create(Identity identity) {
        return persist(identity, true);
    }

    @Override
    public Identity save(Identity identity) {
        return persist(identity, false);
    }

    @Override
    public Optional<Identity> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Identity> findByCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        return jpaRepository.findByCpf(Identity.normalizeCpf(cpf)).map(mapper::toDomain);
    }

    @Override
    public Optional<Identity> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return cpf != null && jpaRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByRg(String rg) {
        return rg != null && jpaRepository.existsByRg(rg);
    }

    @Override
    public boolean existsActiveById(Long id) {
        return jpaRepository.existsByIdAndStatus(id, IdentityStatus.ACTIVE);
    }

    @Override
    public List<Long> findActiveIds() {
        return jpaRepository.findIdsByStatus(IdentityStatus.ACTIVE);
    }

    private Identity persist(Identity identity, boolean flushImmediately) {
        try {
            IdentityEntity entity = mapper.toEntity(identity);
            IdentityEntity saved = flushImmediately
                    ? jpaRepository.saveAndFlush(entity)
                    : jpaRepository.save(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isIdentityUniqueConstraintViolation(exception)) {
                throw new IdentityConflictException(exception);
            }
            throw exception;
        }
    }

    private boolean isIdentityUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                return constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT)
                        .contains("uk_identity_users_");
            }
            cause = cause.getCause();
        }

        return false;
    }
}
