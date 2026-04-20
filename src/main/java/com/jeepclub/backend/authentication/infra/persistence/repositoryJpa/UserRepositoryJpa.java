package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import com.jeepclub.backend.authentication.infra.persistence.entities.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Adaptador Secundário / Adaptador de Repositório (Hexagonal Architecture).
 * Esta classe IMPLEMENTA a interface contida no CORE, mas injeta o componente do Spring.
 * Assim o Core não sabe quem faz as buscas ao banco, mascarando o Spring Data.
 */
@RequiredArgsConstructor
@Repository
public class UserRepositoryJpa implements UserRepository {

    private final UserJpaRepository jpaRepository;


    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        UserEntity savedEntity = jpaRepository.save(entity);
        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                            .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        return jpaRepository.findByCpf(cpf)
                            .map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }
}
