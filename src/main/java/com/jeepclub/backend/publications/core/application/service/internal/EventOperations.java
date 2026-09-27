package com.jeepclub.backend.publications.core.application.service.internal;

import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.*;
import com.jeepclub.backend.publications.core.repository.*;
import com.jeepclub.backend.publications.core.application.exception.EventOperationException;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** Called within application transactions. Every mutation locks the publication root first. */
@Component @RequiredArgsConstructor
public class EventOperations {
    private final PublicationRepository publications;
    private final EventOperationsRepository operations;
    private final EventBillingCommand billing;
    private final EventFinancialQuery financial;
    private final DependentsQuery dependents;
    private final EventVehicleQuery vehicles;
    private final Clock clock;

    public Event locked(Long id) {
        var publication = publications.findByIdForUpdate(id).orElse(null);
        if (!(publication instanceof Event event)) throw error("EVENT_NOT_FOUND");
        return event;
    }
    public Instant now() { return Instant.now(clock); }
    public void requireOpen(Event event) {
        if (event.effectiveStatus(now()) != EventStatus.OPEN) throw error("EVENT_REGISTRATION_CLOSED");
    }
    public EventRegistration registration(Long event, Long user) {
        return operations.registrations(event).stream().filter(r -> r.userId().equals(user)).findFirst()
            .orElseThrow(() -> error("EVENT_REGISTRATION_NOT_FOUND"));
    }
    public EventRegistration register(Event event, Long user, List<EventRegistration.Allocation> allocations, List<Long> unallocatedDependentIds) {
        requireOpen(event);
        if (event.getStatus() != PublicationStatus.PUBLISHED) throw error("EVENT_REGISTRATION_CLOSED");
        if (operations.registrations(event.getId()).stream().anyMatch(r -> r.userId().equals(user))) throw error("EVENT_ALREADY_REGISTERED");
        var rules = operations.rules(event.getId());
        if (rules.stream().anyMatch(r -> r.requiredForParticipation() && !now().isBefore(r.participationCutoff())))
            throw error("EVENT_REGISTRATION_CLOSED");
        var registration = new EventRegistration(null, event.getId(), user, EventRegistration.Status.PENDING_PAYMENT, allocations, now(), null, null, unallocatedDependentIds);
        validateDependents(user, unallocatedDependentIds);
        validateAllocations(user, allocations);
        registration = operations.save(registration);
        for (var rule : rules.stream().sorted(Comparator.comparing(EventChargeRule::chargeDefinitionId)).toList())
            billing.ensureEventMemberCharge(event.getId(), rule.chargeDefinitionId(), user, event.getStartsAt().atZone(ZoneOffset.UTC).toLocalDate(), user);
        return refresh(registration, financial.findByEvent(event.getId()));
    }
    public void validateAllocations(Long user, List<EventRegistration.Allocation> allocations) {
        for (var allocation : allocations) {
            var vehicle = vehicles.findActive(allocation.vehicleId()).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
            if (!vehicle.ownerId().equals(user)) throw error("EVENT_VEHICLE_NOT_OWNED");
            if (allocation.dependentIds().stream().anyMatch(d -> !dependents.isActiveDependentOfUser(d, user))) throw error("EVENT_PARTICIPANT_NOT_FOUND");
            if (allocation.dependentIds().size() + (allocation.member() ? 1 : 0) > vehicle.seatingCapacity()) throw error("EVENT_VEHICLE_CAPACITY_EXCEEDED");
        }
    }
    public void validateDependents(Long user, List<Long> ids) {
        if (ids.stream().anyMatch(d -> !dependents.isActiveDependentOfUser(d,user))) throw error("EVENT_PARTICIPANT_NOT_FOUND");
    }
    public EventRegistration refresh(EventRegistration registration, List<EventFinancialQuery.State> states) {
        return refresh(registration, states, operations.rules(registration.eventId()));
    }
    private EventRegistration refresh(EventRegistration registration, List<EventFinancialQuery.State> states, List<EventChargeRule> rules) {
        if (registration.status() != EventRegistration.Status.PENDING_PAYMENT) return registration;
        boolean satisfied = rules.stream().filter(EventChargeRule::requiredForParticipation).allMatch(rule ->
            states.stream().filter(s -> s.userId().equals(registration.userId()) && s.chargeDefinitionId().equals(rule.chargeDefinitionId()))
                .anyMatch(s -> ("PAID".equals(s.effectiveStatus()) || "PENDING_VALIDATION".equals(s.paymentStatus()))
                    && s.paymentSubmittedAt() != null && !s.paymentSubmittedAt().isAfter(rule.participationCutoff())));
        return satisfied ? operations.save(registration.confirm(now())) : registration;
    }
    public List<EventRegistration> refreshed(Long id) {
        var states = financial.findByEvent(id);
        var rules = operations.rules(id);
        return operations.registrations(id).stream().map(r -> refresh(r, states, rules)).toList();
    }
    public int spare(Long eventId, Long vehicleId) {
        var vehicle = vehicles.findActive(vehicleId).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
        var registrations = operations.registrations(eventId).stream().filter(r -> r.status() != EventRegistration.Status.CANCELLED).toList();
        var owner = registrations.stream().filter(r -> r.userId().equals(vehicle.ownerId()) && r.allocations().stream().anyMatch(a -> a.vehicleId().equals(vehicleId))).findFirst()
            .orElseThrow(() -> error("EVENT_VEHICLE_NOT_OWNED"));
        int occupants = owner.allocations().stream().filter(a -> a.vehicleId().equals(vehicleId)).mapToInt(a -> a.dependentIds().size() + (a.member() ? 1 : 0)).sum();
        long guests = operations.guests(eventId).stream().filter(g -> g.status() == EventGuestRequest.Status.APPROVED && vehicleId.equals(g.vehicleId())).count();
        return vehicle.seatingCapacity() - occupants - (int) guests;
    }
    public void guestSpace(Long eventId, Long vehicleId) {
        if (operations.guests(eventId).stream().anyMatch(g -> g.status() == EventGuestRequest.Status.APPROVED && vehicleId.equals(g.vehicleId()))) throw error("EVENT_GUEST_LIMIT_REACHED");
        if (spare(eventId, vehicleId) < 1) throw error("EVENT_VEHICLE_CAPACITY_EXCEEDED");
    }
    public EventGuestRequest guest(Long event, Long id) {
        return operations.guests(event).stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow(() -> error("EVENT_GUEST_REQUEST_NOT_FOUND"));
    }
    public EventGuestRequest requestGuest(Event event, Long actor, Long vehicle, String cpf, boolean administrative) {
        requireOpen(event);
        if (!administrative) {
            var registration = registration(event.getId(), actor);
            if (registration.status() == EventRegistration.Status.CANCELLED || vehicle == null || registration.allocations().stream().noneMatch(a -> a.vehicleId().equals(vehicle)))
                throw error("EVENT_VEHICLE_NOT_OWNED");
            guestSpace(event.getId(), vehicle);
        }
        var guest = new EventGuestRequest(null, event.getId(), actor, vehicle, cpf, EventGuestRequest.Status.PENDING, administrative, null, now(), null, null);
        if (operations.guests(event.getId()).stream().anyMatch(g -> g.cpf().equals(guest.cpf()))) throw error("EVENT_GUEST_ALREADY_EXISTS");
        return operations.save(guest);
    }
    public EventGuestRequest reviewGuest(Event event, Long guestId, boolean approve, Long vehicle, Long actor, String reason) {
        requireOpen(event);
        var guest = guest(event.getId(), guestId);
        if (guest.status() != EventGuestRequest.Status.PENDING) throw error("EVENT_GUEST_REQUEST_PROCESSED");
        Long chosen = vehicle == null ? guest.vehicleId() : vehicle;
        if (approve) {
            if (chosen == null) throw error("EVENT_RIDE_OFFER_INVALID");
            var registrations = refreshed(event.getId());
            Long targetVehicle = chosen;
            if (registrations.stream().noneMatch(r -> r.status() == EventRegistration.Status.CONFIRMED
                    && r.allocations().stream().anyMatch(a -> a.vehicleId().equals(targetVehicle)))) throw error("EVENT_PAYMENT_REQUIRED");
            guestSpace(event.getId(), chosen);
        }
        return operations.save(guest.review(approve, chosen, actor, reason, now()));
    }
    public static EventOperationException error(String code) { return new EventOperationException(code); }
}
