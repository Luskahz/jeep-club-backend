package com.jeepclub.backend.iam.identity.infra.persistence.adapter;

import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User create(User user) {
        return persist(user, true);
    }

    @Override
    public User save(User user) {
        return persist(user, false);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        return jpaRepository.findByCpf(User.normalizeCpf(cpf)).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
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
        return jpaRepository.existsByIdAndStatus(id, UserStatus.ACTIVE);
    }

    @Override
    public List<Long> findActiveIds() {
        return jpaRepository.findIdsByStatus(UserStatus.ACTIVE);
    }

    private User persist(User user, boolean flushImmediately) {
        try {
            UserEntity entity = mapper.toEntity(user);
            UserEntity saved = flushImmediately
                    ? jpaRepository.saveAndFlush(entity)
                    : jpaRepository.save(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isUserUniqueConstraintViolation(exception)) {
                throw new UserConflictException(exception);
            }
            throw exception;
        }
    }

    private boolean isUserUniqueConstraintViolation(Throwable exception) {
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
