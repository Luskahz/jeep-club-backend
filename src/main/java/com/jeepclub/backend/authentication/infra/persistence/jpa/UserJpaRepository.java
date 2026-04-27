package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório nativo do Spring Data JPA (Infra).
 * É injetado pelo adaptador (UserRepositoryJpa) para fazer a ponte com o banco
 * via EntityManager do Spring.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

}
