package com.jeepclub.backend.memberships.infra.persistence.jpa;
import com.jeepclub.backend.memberships.infra.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemberJpaRepository extends JpaRepository<MemberEntity,Long> {
    // A current locking read is essential under MySQL REPEATABLE READ after a mutex wait.
    @org.springframework.data.jpa.repository.Query(value="SELECT membership_number FROM membership_members ORDER BY CAST(membership_number AS DECIMAL(65,0)) DESC LIMIT 1 FOR UPDATE",nativeQuery=true)
    Optional<String> greatestCurrentNumberForUpdate();
    Optional<MemberEntity> findByIdentityUserId(Long userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MemberEntity m where m.identityUserId=:id")
    Optional<MemberEntity> lockByIdentity(Long id);
    Optional<MemberEntity> findByMembershipNumber(String number);
    List<MemberEntity> findByIdentityUserIdIn(Collection<Long> ids);
}
