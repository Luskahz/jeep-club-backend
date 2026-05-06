package com.jeepclub.backend.authorization.infra.persistence.repository.jpa;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.authorization.infra.persistence.jpa.RoleJpaRepository;
import com.jeepclub.backend.authorization.infra.persistence.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryJpa implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final RoleMapper mapper;

    @Override
    public List<Role> findAll() {
        return jpa.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpa.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = mapper.toEntity(role);

        RoleEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }
}