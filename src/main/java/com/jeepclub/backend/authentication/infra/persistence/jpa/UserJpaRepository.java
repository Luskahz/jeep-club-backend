package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositório nativo do Spring Data JPA (Infra).
 * É injetado pelo adaptador (UserRepositoryJpa) para fazer a ponte com o banco
 * via EntityManager do Spring.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByIdAndStatus(
            Long id,
            UserStatus status
    );

    @Query("""
            select user.id
            from UserEntity user
            where user.status = com.jeepclub.backend.authentication.core.domain.enums.UserStatus.ACTIVE
            order by user.id
            """)
    List<Long> findActiveUserIds();
}