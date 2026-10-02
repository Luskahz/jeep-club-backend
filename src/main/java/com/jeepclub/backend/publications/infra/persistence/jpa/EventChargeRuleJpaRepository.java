package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventChargeRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventChargeRuleJpaRepository extends JpaRepository<EventChargeRuleEntity, Long> {
    List<EventChargeRuleEntity> findByEventId(Long eventId);

}
