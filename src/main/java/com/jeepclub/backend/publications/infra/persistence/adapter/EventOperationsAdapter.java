package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.EventOperationsRepository;
import com.jeepclub.backend.publications.infra.persistence.entity.*;
import com.jeepclub.backend.publications.infra.persistence.jpa.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository @RequiredArgsConstructor
public class EventOperationsAdapter implements EventOperationsRepository {
    private final EventChargeRuleJpaRepository rules;
    private final EventRegistrationJpaRepository registrations;
    private final EventGuestRequestJpaRepository guests;
    private final EventRideOfferJpaRepository offers;
    private final EventGuestTransitionJpaRepository transitions;
    private final jakarta.persistence.EntityManager entityManager;
    private final EventCurrentIds currentIds;

    /** Event root is already held by the caller. A short separate read avoids an
     * old RR snapshot without locking child rows before Vehicles/Dependents. */
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
        isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED,readOnly=true)
    public References committedReferences(Long eventId) {
        var vehicleIds = ids("""
            select v.vehicle_id from event_registration_vehicles v join event_registrations r on r.id=v.registration_id where r.event_id=:event
            union select g.vehicle_id from event_guest_requests g where g.event_id=:event and g.vehicle_id is not null
            union select g.requested_vehicle_id from event_guest_requests g where g.event_id=:event and g.requested_vehicle_id is not null
            union select o.vehicle_id from event_ride_offers o where o.event_id=:event
            """,eventId);
        var dependentIds = ids("""
            select o.dependent_id from event_registration_occupants o join event_registrations r on r.id=o.registration_id where r.event_id=:event and o.dependent_id is not null
            union select d.dependent_id from event_registration_unallocated_dependents d join event_registrations r on r.id=d.registration_id where r.event_id=:event
            """,eventId);
        return new References(vehicleIds,dependentIds);
    }
    @SuppressWarnings("unchecked")
    private List<Long> ids(String sql,Long eventId) {
        return ((List<Number>)entityManager.createNativeQuery(sql).setParameter("event",eventId).getResultList())
            .stream().map(Number::longValue).sorted().toList();
    }

    private boolean readOnly() {
        return org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }
    private <T> List<T> current(Long event,Class<T> type,java.util.function.Supplier<List<T>> cachedQuery,
            java.util.function.Function<Collection<Long>,List<T>> lockingQuery,java.util.function.Function<T,Long> id) {
        entityManager.flush();
        var cached=cachedQuery.get();
        var ids=new TreeSet<>(currentIds.read(type,event));
        cached.forEach(row -> {ids.add(id.apply(row));entityManager.detach(row);});
        return ids.isEmpty()?List.of():lockingQuery.apply(ids).stream().distinct().toList();
    }

    public List<EventChargeRule> rules(Long eventId) {
        return rules.findByEventId(eventId).stream().map(e -> new EventChargeRule(e.getEventId(), e.getChargeDefinitionId(), e.isRequiredForParticipation(), e.getParticipationCutoff(), e.getFinancialDueDate())).toList();
    }
    public void replaceRules(Long eventId, List<EventChargeRule> values) {
        rules.deleteAll(rules.findByEventId(eventId)); rules.flush();
        for (var value : values) {
            var e = new EventChargeRuleEntity(); e.setEventId(eventId); e.setChargeDefinitionId(value.chargeDefinitionId());
            e.setRequiredForParticipation(value.requiredForParticipation()); e.setParticipationCutoff(value.participationCutoff());
            e.setFinancialDueDate(value.financialDueDate());
            rules.save(e);
        }
        rules.flush();
    }
    public List<EventRegistration> registrations(Long eventId) {
        if (readOnly()) return registrations.findByEventId(eventId).stream().map(this::registration).toList();
        var rows = current(eventId,EventRegistrationEntity.class,() -> registrations.findByEventId(eventId),registrations::findCurrentByIds,EventRegistrationEntity::getId);
        if(rows.isEmpty())return List.of();
        var ids=rows.stream().map(EventRegistrationEntity::getId).toList();
        var vehicleRows=registrations.currentVehicleRows(ids);
        var occupants=registrations.currentOccupantRows(ids);
        var unallocated=registrations.currentUnallocatedRows(ids);
        return rows.stream().map(e -> {
            var allocations=vehicleRows.stream().filter(v -> number(v[0]).equals(e.getId())).map(v -> {
                Long vehicle=number(v[1]);
                var people=occupants.stream().filter(o -> number(o[0]).equals(e.getId()) && number(o[1]).equals(vehicle)).toList();
                return new EventRegistration.Allocation(vehicle,people.stream().anyMatch(o -> o[2]==null),
                    people.stream().filter(o -> o[2]!=null).map(o -> number(o[2])).toList());
            }).toList();
            return new EventRegistration(e.getId(),e.getEventId(),e.getUserId(),EventRegistration.Status.valueOf(e.getStatus()),
                allocations,e.getCreatedAt(),e.getConfirmedAt(),e.getCancelledAt(),
                unallocated.stream().filter(d -> number(d[0]).equals(e.getId())).map(d -> number(d[1])).toList());
        }).toList();
    }
    private static Long number(Object value) { return ((Number)value).longValue(); }
    public EventRegistration save(EventRegistration r) {
        var e = r.id() == null ? new EventRegistrationEntity() : registrations.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setUserId(r.userId()); e.setStatus(r.status().name());
        e.setCreatedAt(r.createdAt()); e.setConfirmedAt(r.confirmedAt()); e.setCancelledAt(r.cancelledAt());
        // Replace whole value collections through JPA; do not diff against a
        // possibly old lazy collection snapshot after a database lock wait.
        e.setUnallocatedDependentIds(new ArrayList<>(r.unallocatedDependentIds()));
        e.setOccupants(new ArrayList<>()); e.setVehicleIds(new HashSet<>());
        for (var a : r.allocations()) {
            e.getVehicleIds().add(a.vehicleId());
            if (a.member()) e.getOccupants().add(new EventOccupantEmbeddable(a.vehicleId(), "MEMBER", null));
            for (Long dependent : a.dependentIds()) e.getOccupants().add(new EventOccupantEmbeddable(a.vehicleId(), "DEPENDENT-" + dependent, dependent));
        }
        return r.identified(registrations.saveAndFlush(e).getId());
    }
    private EventRegistration registration(EventRegistrationEntity e) {
        var allocations = e.getVehicleIds().stream().sorted().map(v -> new EventRegistration.Allocation(v,
            e.getOccupants().stream().anyMatch(o -> o.getVehicleId().equals(v) && o.getDependentId() == null),
            e.getOccupants().stream().filter(o -> o.getVehicleId().equals(v) && o.getDependentId() != null).map(EventOccupantEmbeddable::getDependentId).toList())).toList();
        return new EventRegistration(e.getId(), e.getEventId(), e.getUserId(), EventRegistration.Status.valueOf(e.getStatus()),
            allocations, e.getCreatedAt(), e.getConfirmedAt(), e.getCancelledAt(), e.getUnallocatedDependentIds());
    }
    public List<EventGuestRequest> guests(Long id) {
        var rows = readOnly() ? guests.findByEventId(id) : current(id,EventGuestRequestEntity.class,() -> guests.findByEventId(id),guests::findCurrentByIds,EventGuestRequestEntity::getId);
        return rows.stream().map(e -> new EventGuestRequest(e.getId(), e.getEventId(), e.getRequesterUserId(),
            e.getVehicleId(), e.getCpf(), EventGuestRequest.Status.valueOf(e.getStatus()), e.isAdministrative(), e.getReviewerId(), e.getCreatedAt(), e.getReviewedAt(), e.getRejectionReason(), e.getGuestName(),
            e.getOriginRegistrationId(),e.getRequestedVehicleId(),e.getAllocatedRegistrationId(),e.getResponsibleUserId(),e.getSelectedOfferId())).toList();
    }
    public EventGuestRequest save(EventGuestRequest r) {
        var e = r.id() == null ? new EventGuestRequestEntity() : guests.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setRequesterUserId(r.requesterUserId()); e.setVehicleId(r.vehicleId());
        e.setOriginRegistrationId(r.originRegistrationId()); e.setRequestedVehicleId(r.requestedVehicleId());
        e.setAllocatedRegistrationId(r.allocatedRegistrationId()); e.setResponsibleUserId(r.responsibleUserId()); e.setSelectedOfferId(r.selectedOfferId());
        e.setApprovedVehicleId(r.status() == EventGuestRequest.Status.APPROVED ? r.vehicleId() : null);
        e.setGuestName(r.guestName());
        e.setCpf(r.cpf()); e.setStatus(r.status().name()); e.setAdministrative(r.administrative());
        e.setReviewerId(r.reviewerId()); e.setCreatedAt(r.createdAt()); e.setReviewedAt(r.reviewedAt()); e.setRejectionReason(r.rejectionReason());
        return r.identified(guests.saveAndFlush(e).getId());
    }
    public long approvedVisits(String cpf) { return guests.countByCpfAndStatus(cpf, "APPROVED"); }
    public Map<String,Long> approvedVisits(Collection<String> cpfs) {
        var result = new HashMap<String,Long>();
        if (!cpfs.isEmpty()) guests.approvedVisits(cpfs).forEach(row -> result.put((String) row[0], (Long) row[1]));
        return Map.copyOf(result);
    }
    public List<EventRideOffer> offers(Long id) {
        var rows = readOnly() ? offers.findByEventId(id) : current(id,EventRideOfferEntity.class,() -> offers.findByEventId(id),offers::findCurrentByIds,EventRideOfferEntity::getId);
        return rows.stream().map(e -> new EventRideOffer(e.getId(), e.getEventId(), e.getGuestRequestId(), e.getRegistrationId(),
            e.getUserId(), e.getVehicleId(), EventRideOffer.Status.valueOf(e.getStatus()), e.getRespondedAt(), e.getSelectedAt(),e.getWithdrawnAt())).toList();
    }
    public EventRideOffer save(EventRideOffer r) {
        var e = r.id() == null ? new EventRideOfferEntity() : offers.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setGuestRequestId(r.guestRequestId()); e.setRegistrationId(r.registrationId());
        e.setSelectedGuestId(r.status() == EventRideOffer.Status.SELECTED ? r.guestRequestId() : null);
        e.setUserId(r.userId()); e.setVehicleId(r.vehicleId()); e.setStatus(r.status().name());
        e.setRespondedAt(r.respondedAt()); e.setSelectedAt(r.selectedAt());
        e.setWithdrawnAt(r.withdrawnAt());
        return r.identified(offers.saveAndFlush(e).getId());
    }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void record(EventGuestTransition t) {
        if (t.id() != null) throw new IllegalArgumentException("History is append only.");
        var e = new EventGuestTransitionEntity();
        e.setEventId(t.eventId()); e.setGuestRequestId(t.guestRequestId()); e.setAction(t.action());
        e.setActorUserId(t.actorUserId()); e.setReason(t.reason()); e.setOccurredAt(t.occurredAt());
        e.setPreviousStatus(t.previousStatus()); e.setNextStatus(t.nextStatus());
        e.setPreviousVehicleId(t.previousVehicleId()); e.setNextVehicleId(t.nextVehicleId());
        e.setPreviousRegistrationId(t.previousRegistrationId()); e.setNextRegistrationId(t.nextRegistrationId()); e.setOfferId(t.offerId());
        transitions.save(e);
    }
    public List<EventGuestTransition> history(Long eventId,Long guestId) {
        return transitions.findByEventIdAndGuestRequestIdOrderById(eventId,guestId).stream().map(e ->
            new EventGuestTransition(e.getId(),e.getEventId(),e.getGuestRequestId(),e.getAction(),e.getActorUserId(),e.getReason(),e.getOccurredAt(),
                e.getPreviousStatus(),e.getNextStatus(),e.getPreviousVehicleId(),e.getNextVehicleId(),e.getPreviousRegistrationId(),e.getNextRegistrationId(),e.getOfferId())).toList();
    }
}
