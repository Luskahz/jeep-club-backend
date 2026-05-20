package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordRecoveryRequestJpaRepository extends JpaRepository<PasswordRecoveryRequestEntity, Long> {

    Optional<PasswordRecoveryRequestEntity> findByTokenHash(String tokenHash);
}