package com.jeepclub.backend.memberships.infra.persistence.jpa;
import com.jeepclub.backend.memberships.infra.persistence.entity.MemberRegistrySequenceEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface MemberRegistrySequenceJpaRepository extends JpaRepository<MemberRegistrySequenceEntity,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MemberRegistrySequenceEntity s where s.id=1")
    Optional<MemberRegistrySequenceEntity> lockCurrent();
}
