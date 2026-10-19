package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRegistrationJpaRepository extends JpaRepository<EventRegistrationEntity, Long> {
    List<EventRegistrationEntity> findByEventId(Long eventId);


    @org.springframework.data.jpa.repository.Query(value="select x.registration_id from event_registration_occupants x join event_registrations r on r.id=x.registration_id where x.dependent_id=:id and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockDependentAllocation(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value="select x.registration_id from event_registration_unallocated_dependents x join event_registrations r on r.id=x.registration_id where x.dependent_id=:id and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockUnallocatedDependent(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value="select x.registration_id from event_registration_vehicles x join event_registrations r on r.id=x.registration_id where x.vehicle_id=:id and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockVehicleAllocation(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value="select x.registration_id from event_registration_occupants x join event_registrations r on r.id=x.registration_id where x.vehicle_id=:id and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockVehicleOccupant(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value="select r.id from event_guest_requests r where (vehicle_id=:id or approved_vehicle_id=:id) and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockVehicleGuest(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value="select r.id from event_ride_offers r where (vehicle_id=:id) and (exists (select 1 from publications p where p.id=r.event_id) or not exists (select 1 from publication_history h where h.publication_id=r.event_id)) limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockVehicleOffer(@org.springframework.data.repository.query.Param("id") Long id);
}
