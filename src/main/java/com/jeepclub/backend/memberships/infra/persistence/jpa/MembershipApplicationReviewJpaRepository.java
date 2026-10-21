package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MembershipApplicationReviewJpaRepository extends JpaRepository<MembershipApplicationReviewEntity, Long> {
    List<MembershipApplicationReviewEntity> findByApplicationIdOrderById(Long applicationId);
}
