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

    public List<EventChargeRule> rules(Long eventId) {
        return rules.findByEventId(eventId).stream().map(e -> new EventChargeRule(e.getEventId(), e.getChargeDefinitionId(), e.isRequiredForParticipation(), e.getParticipationCutoff())).toList();
    }
    public void replaceRules(Long eventId, List<EventChargeRule> values) {
        rules.deleteAll(rules.findByEventId(eventId)); rules.flush();
        for (var value : values) {
            var e = new EventChargeRuleEntity(); e.setEventId(eventId); e.setChargeDefinitionId(value.chargeDefinitionId());
            e.setRequiredForParticipation(value.requiredForParticipation()); e.setParticipationCutoff(value.participationCutoff());
            rules.save(e);
        }
        rules.flush();
    }
    public List<EventRegistration> registrations(Long eventId) {
        return registrations.findByEventId(eventId).stream().map(this::registration).toList();
    }
    public EventRegistration save(EventRegistration r) {
        var e = r.id() == null ? new EventRegistrationEntity() : registrations.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setUserId(r.userId()); e.setStatus(r.status().name());
        e.setCreatedAt(r.createdAt()); e.setConfirmedAt(r.confirmedAt()); e.setCancelledAt(r.cancelledAt());
        e.getUnallocatedDependentIds().clear(); e.getUnallocatedDependentIds().addAll(r.unallocatedDependentIds());
        e.getOccupants().clear(); e.getVehicleIds().clear();
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
        return guests.findByEventId(id).stream().map(e -> new EventGuestRequest(e.getId(), e.getEventId(), e.getRequesterUserId(),
            e.getVehicleId(), e.getCpf(), EventGuestRequest.Status.valueOf(e.getStatus()), e.isAdministrative(), e.getReviewerId(), e.getCreatedAt(), e.getReviewedAt(), e.getRejectionReason())).toList();
    }
    public EventGuestRequest save(EventGuestRequest r) {
        var e = r.id() == null ? new EventGuestRequestEntity() : guests.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setRequesterUserId(r.requesterUserId()); e.setVehicleId(r.vehicleId());
        e.setApprovedVehicleId(r.status() == EventGuestRequest.Status.APPROVED ? r.vehicleId() : null);
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
        return offers.findByEventId(id).stream().map(e -> new EventRideOffer(e.getId(), e.getEventId(), e.getGuestRequestId(), e.getRegistrationId(),
            e.getUserId(), e.getVehicleId(), EventRideOffer.Status.valueOf(e.getStatus()), e.getRespondedAt(), e.getSelectedAt())).toList();
    }
    public EventRideOffer save(EventRideOffer r) {
        var e = r.id() == null ? new EventRideOfferEntity() : offers.findById(r.id()).orElseThrow();
        e.setEventId(r.eventId()); e.setGuestRequestId(r.guestRequestId()); e.setRegistrationId(r.registrationId());
        e.setSelectedGuestId(r.status() == EventRideOffer.Status.SELECTED ? r.guestRequestId() : null);
        e.setUserId(r.userId()); e.setVehicleId(r.vehicleId()); e.setStatus(r.status().name());
        e.setRespondedAt(r.respondedAt()); e.setSelectedAt(r.selectedAt());
        return r.identified(offers.saveAndFlush(e).getId());
    }
}
