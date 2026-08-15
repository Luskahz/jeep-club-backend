package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository
        extends JpaRepository<UserEntity, Long>{

    Optional<UserEntity> findByCpf(String cpf);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM UserEntity user
            WHERE user.id = :id
            """)
    Optional<UserEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM UserEntity user
            WHERE user.cpf = :cpf
            """)
    Optional<UserEntity> findByCpfForUpdate(
            @Param("cpf") String cpf
    );

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsByIdAndAccountStatusAndAuthenticationStatusAndCredentialStatus(
            Long id,
            AccountStatus accountStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus
    );

    @Query("""
            SELECT user.id
            FROM UserEntity user
            WHERE user.accountStatus =
                com.jeepclub.backend.authentication.core.domain.enums.AccountStatus.ACTIVE
              AND user.authenticationStatus =
                com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus.ENABLED
              AND user.credentialStatus =
                com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus.PERMANENT
            ORDER BY user.id
            """)
    List<Long> findActiveUserIds();
}