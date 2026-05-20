package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordResetRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRequestJpaRepository extends JpaRepository<PasswordResetRequestEntity, Long> {

    Optional<PasswordResetRequestEntity> findByTokenHash(String tokenHash);
}