package com.jeepclub.backend.authentication.infra.persistence.repository;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordResetRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPasswordResetRequestRepository extends JpaRepository<PasswordResetRequestEntity, Long> {
    Optional<PasswordResetRequestEntity> findByTokenHash(String tokenHash);
}
