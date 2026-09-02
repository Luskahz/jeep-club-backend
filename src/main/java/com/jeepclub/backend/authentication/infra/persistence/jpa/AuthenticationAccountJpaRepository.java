package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthenticationAccountJpaRepository
        extends JpaRepository<AuthenticationAccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM AuthenticationAccountEntity account
            WHERE account.identityId = :identityId
            """)
    Optional<AuthenticationAccountEntity> findByIdentityIdForUpdate(
            @Param("identityId") Long identityId
    );
}
