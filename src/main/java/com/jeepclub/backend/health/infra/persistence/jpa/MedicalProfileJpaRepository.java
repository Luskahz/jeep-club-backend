package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MedicalProfileJpaRepository extends JpaRepository<MedicalProfileEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from MedicalProfileEntity m
            where m.id = :id
            """)
    Optional<MedicalProfileEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    Optional<MedicalProfileEntity> findByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from MedicalProfileEntity m
            where m.ownerType = :ownerType
              and m.ownerId = :ownerId
            """)
    Optional<MedicalProfileEntity> findByOwnerForUpdate(
            @Param("ownerType") MedicalProfileOwnerType ownerType,
            @Param("ownerId") Long ownerId
    );

    boolean existsByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    @Query(
            value = """
                    select m from MedicalProfileEntity m where
                    (m.ownerType = :userOwnerType and exists (select 1 from com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity u where u.id = m.ownerId and u.status = :activeUserStatus))
                    or (m.ownerType = :dependentOwnerType and exists (select 1 from com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity d where d.id = m.ownerId and d.status = :activeDependentStatus))
                    """,
            countQuery = """
                    select count(m) from MedicalProfileEntity m where
                    (m.ownerType = :userOwnerType and exists (select 1 from com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity u where u.id = m.ownerId and u.status = :activeUserStatus))
                    or (m.ownerType = :dependentOwnerType and exists (select 1 from com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity d where d.id = m.ownerId and d.status = :activeDependentStatus))
                    """
    )
    Page<MedicalProfileEntity> findAllWithActiveOwners(
            Pageable pageable,
            @Param("userOwnerType") MedicalProfileOwnerType userOwnerType,
            @Param("dependentOwnerType") MedicalProfileOwnerType dependentOwnerType,
            @Param("activeUserStatus") UserStatus activeUserStatus,
            @Param("activeDependentStatus") DependentStatus activeDependentStatus
    );
}
