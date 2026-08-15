package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.UserMapper;
import com.jeepclub.backend.authentication.infra.persistence.sort.UserSortMapper;
import com.jeepclub.backend.authentication.infra.persistence.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter
        implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User create(User user) {
        try {
            UserEntity entity =
                    UserMapper.toEntity(user);

            UserEntity savedEntity =
                    jpaRepository.saveAndFlush(entity);

            return UserMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            if (isRegistrationUniqueConflict(exception)) {
                throw new RegistrationConflictException();
            }
            throw exception;
        }
    }

    @Override
    public User save(User user) {
        UserEntity entity =
                UserMapper.toEntity(user);

        UserEntity savedEntity =
                jpaRepository.save(entity);

        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        return jpaRepository.findByCpf(cpf)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByCpfForUpdate(String cpf) {
        return jpaRepository.findByCpfForUpdate(cpf)
                .map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
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
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<Long> findActiveUserIds() {
        return jpaRepository.findActiveUserIds();
    }

    @Override
    public boolean existsActiveById(Long id) {
        return jpaRepository.existsByIdAndAccountStatusAndAuthenticationStatusAndCredentialStatus(
                id,
                AccountStatus.ACTIVE,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PERMANENT
        );
    }

    @Override
    public Page<User> findAll(
            AdminUserFilter filter,
            Pageable pageable
    ) {
        Pageable mappedPageable =
                UserSortMapper.map(pageable);

        return jpaRepository
                .findAll(
                        UserSpecification.from(filter),
                        mappedPageable
                )
                .map(UserMapper::toDomain);
    }

    private boolean isRegistrationUniqueConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("authentication_users")
                && (normalized.contains("cpf")
                || normalized.contains("email")
                || normalized.contains("rg"));
    }
}
