package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.EventGuestTransitionEntity;
import org.springframework.data.repository.Repository;
import java.util.List;

/** Append/read only. History deliberately has no deletion command. */
public interface EventGuestTransitionJpaRepository extends Repository<EventGuestTransitionEntity, Long> {
    EventGuestTransitionEntity save(EventGuestTransitionEntity value);
    List<EventGuestTransitionEntity> findByEventIdAndGuestRequestIdOrderById(Long eventId,Long guestRequestId);
}
