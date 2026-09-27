package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRegistrationJpaRepository extends JpaRepository<EventRegistrationEntity, Long> {
    List<EventRegistrationEntity> findByEventId(Long eventId);

}
