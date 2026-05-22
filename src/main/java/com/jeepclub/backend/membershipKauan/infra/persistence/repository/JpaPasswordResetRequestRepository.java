package com.jeepclub.backend.membershipKauan.infra.persistence.repository;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPasswordResetRequestRepository extends JpaRepository<PasswordRecoveryRequestEntity, Long> {
    Optional<PasswordRecoveryRequestEntity> findByTokenHash(String tokenHash);
}
