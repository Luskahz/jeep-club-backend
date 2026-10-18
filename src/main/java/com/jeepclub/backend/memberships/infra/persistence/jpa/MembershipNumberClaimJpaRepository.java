package com.jeepclub.backend.memberships.infra.persistence.jpa;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipNumberClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MembershipNumberClaimJpaRepository extends JpaRepository<MembershipNumberClaimEntity,String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from MembershipNumberClaimEntity c where c.claimKey=:key")
    java.util.Optional<MembershipNumberClaimEntity> lockByKey(String key);
}
