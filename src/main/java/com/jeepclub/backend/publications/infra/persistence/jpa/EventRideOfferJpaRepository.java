package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventRideOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRideOfferJpaRepository extends JpaRepository<EventRideOfferEntity, Long> {
    List<EventRideOfferEntity> findByEventId(Long eventId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from EventRideOfferEntity o where o.id in :ids order by o.id")
    List<EventRideOfferEntity> findCurrentByIds(java.util.Collection<Long> ids);

}
