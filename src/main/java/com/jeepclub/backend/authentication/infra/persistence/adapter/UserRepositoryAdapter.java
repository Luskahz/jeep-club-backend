package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
            throw new RegistrationConflictException();
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
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<Long> findActiveUserIds() {
        return jpaRepository.findActiveUserIds();
    }

    @Override
    public boolean existsActiveById(Long id) {
        return jpaRepository.existsByIdAndStatus(
                id,
                UserStatus.ACTIVE
        );
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(UserMapper::toDomain)
                .toList();
    }
}