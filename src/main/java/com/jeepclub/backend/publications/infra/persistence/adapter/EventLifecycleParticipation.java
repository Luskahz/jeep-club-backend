package com.jeepclub.backend.publications.infra.persistence.adapter;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/** Nonlocking MVCC reads under the caller's Vehicle/Dependent mutex. Event writers
 * must take the same mutex before changing references. Never lock Event or child
 * rows here: Event commands take those locks before the lifecycle mutex. */
@Component @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class EventLifecycleParticipation {
    private final EntityManager entityManager;
    private final EventCurrentIds committedSnapshots;
    private static final String OPERATIONAL_EVENT="""
        e.event_status not in ('CANCELLED','FINISHED') and (e.ends_at is null or e.ends_at>:now)
        """;

    public boolean vehicle(Long id,Instant now) {
        return current(() -> exists("""
            select 1 from publication_events e where
            """+OPERATIONAL_EVENT+"""
            and (
              exists (select 1 from event_registrations r join event_registration_vehicles v on v.registration_id=r.id
                where r.event_id=e.publication_id and r.status in ('PENDING_PAYMENT','CONFIRMED') and v.vehicle_id=:id)
              or exists (select 1 from event_registrations r join event_registration_occupants o on o.registration_id=r.id
                where r.event_id=e.publication_id and r.status in ('PENDING_PAYMENT','CONFIRMED') and o.vehicle_id=:id)
              or exists (select 1 from event_guest_requests g where g.event_id=e.publication_id and
                ((g.status='PENDING' and g.requested_vehicle_id=:id) or (g.status='APPROVED' and g.vehicle_id=:id)))
              or exists (select 1 from event_ride_offers o where o.event_id=e.publication_id and o.vehicle_id=:id
                and o.status in ('ACCEPTED','SELECTED'))
            )
            """,id,now));
    }
    private boolean exists(String sql,Long id,Instant now) {
        return !entityManager.createNativeQuery(sql).setParameter("id",id).setParameter("now",now)
            .setMaxResults(1).getResultList().isEmpty();
    }
    private boolean current(Supplier<Boolean> read) {
        // A pre-existing caller may have established an old REPEATABLE_READ snapshot.
        return Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),
            java.sql.Connection.TRANSACTION_READ_COMMITTED) ? read.get() : committedSnapshots.inCommittedSnapshot(read);
    }
}
