package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventRideOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRideOfferJpaRepository extends JpaRepository<EventRideOfferEntity, Long> {
    List<EventRideOfferEntity> findByEventId(Long eventId);

}
