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
    private final com.jeepclub.backend.dependents.api.module.DependentLifecycleLock dependentLocks;
    private final com.jeepclub.backend.vehicles.api.module.VehicleLifecycleLock vehicleLocks;

    public Event locked(Long id) {
        return locked(id,List.of(),List.of());
    }
    public Event locked(Long id,Collection<Long> addedVehicles,Collection<Long> addedDependents) {
        var event = publications.findEventByIdForUpdate(id).orElseThrow(() -> error("EVENT_NOT_FOUND"));
        var references = operations.currentReferences(id);
        java.util.stream.Stream.concat(references.vehicleIds().stream(),addedVehicles.stream()).distinct().sorted()
            .forEach(vehicleLocks::activeById);
        java.util.stream.Stream.concat(references.dependentIds().stream(),addedDependents.stream()).distinct().sorted()
            .forEach(dependentLocks::byId);
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
        lockReferences(user,allocations,unallocatedDependentIds);
        validateDependents(user, unallocatedDependentIds);
        validateAllocations(user, allocations);
        registration = operations.save(registration);
        for (var rule : rules.stream().sorted(Comparator.comparing(EventChargeRule::chargeDefinitionId)).toList())
            billing.ensureEventMemberCharge(event.getId(), rule.chargeDefinitionId(), user, resolveFinancialDueDate(rule, event), user);
        return refresh(registration, financial.findByEvent(event.getId()));
    }
    private LocalDate resolveFinancialDueDate(EventChargeRule rule, Event event) {
        return rule.financialDueDate() != null ? rule.financialDueDate()
            : event.getStartsAt().atZone(ZoneOffset.UTC).toLocalDate();
    }
    public void validateAllocations(Long user, List<EventRegistration.Allocation> allocations) {
        for (var allocation : allocations) {
            var vehicle = vehicleLocks.activeById(allocation.vehicleId()).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
            if (!vehicle.ownerId().equals(user)) throw error("EVENT_VEHICLE_NOT_OWNED");
            if (allocation.dependentIds().stream().anyMatch(d -> !activeDependent(d,user))) throw error("EVENT_PARTICIPANT_NOT_FOUND");
            if (allocation.dependentIds().size() + (allocation.member() ? 1 : 0) > vehicle.seatingCapacity()) throw error("EVENT_VEHICLE_CAPACITY_EXCEEDED");
        }
    }
    public void validateDependents(Long user, List<Long> ids) {
        if (ids.stream().anyMatch(d -> !activeDependent(d,user))) throw error("EVENT_PARTICIPANT_NOT_FOUND");
    }
    public EventRegistration refresh(EventRegistration registration, List<EventFinancialQuery.State> states) {
        return refresh(registration, states, operations.rules(registration.eventId()));
    }
    private EventRegistration refresh(EventRegistration registration, List<EventFinancialQuery.State> states, List<EventChargeRule> rules) {
        if (registration.status() != EventRegistration.Status.PENDING_PAYMENT) return registration;
        boolean satisfied = rules.stream().filter(EventChargeRule::requiredForParticipation).allMatch(rule ->
            states.stream().filter(s -> s.userId().equals(registration.userId()) && s.chargeDefinitionId().equals(rule.chargeDefinitionId()))
                .anyMatch(s -> EventParticipationRequirement.satisfied(rule, s)));
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
        vehicleLocks.activeById(vehicleId).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
        if (spare(eventId, vehicleId) < 1) throw error("EVENT_VEHICLE_CAPACITY_EXCEEDED");
    }
    public EventGuestRequest guest(Long event, Long id) {
        return operations.guests(event).stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow(() -> error("EVENT_GUEST_REQUEST_NOT_FOUND"));
    }
    public EventGuestRequest requestGuest(Event event, Long actor, Long vehicle, String cpf, boolean administrative) {
        return requestGuest(event,actor,vehicle,cpf,administrative,null);
    }
    public EventGuestRequest requestGuest(Event event, Long actor, Long vehicle, String cpf, boolean administrative, String guestName) {
        requireOpen(event);
        EventGuestRequest guest;
        if (!administrative) {
            var registration = registration(event.getId(), actor);
            if (registration.status() == EventRegistration.Status.CANCELLED || vehicle == null || registration.allocations().stream().noneMatch(a -> a.vehicleId().equals(vehicle)))
                throw error("EVENT_VEHICLE_NOT_OWNED");
            // A request is a preference, never a seat reservation. Capacity is
            // checked only when approval actually allocates the guest.
            var requestedVehicle = vehicleLocks.activeById(vehicle).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
            if (!requestedVehicle.ownerId().equals(actor)) throw error("EVENT_VEHICLE_NOT_OWNED");
            guest = EventGuestRequest.requested(event.getId(),actor,registration.id(),vehicle,cpf,guestName,now());
        } else {
            if (vehicle != null) throw error("EVENT_RIDE_OFFER_INVALID");
            guest = EventGuestRequest.standby(event.getId(),actor,cpf,guestName,now());
        }
        var saved = operations.save(guest);
        record(null,saved,"CREATED",actor,"GUEST_CREATED",null);
        return saved;
    }
    public EventGuestRequest reviewGuest(Event event, Long guestId, boolean approve, Long vehicle, Long actor, String reason) {
        requireOpen(event);
        var guest = guest(event.getId(), guestId);
        if (guest.administrative() || guest.status() != EventGuestRequest.Status.PENDING) throw error("EVENT_GUEST_REQUEST_PROCESSED");
        if (vehicle != null && !vehicle.equals(guest.requestedVehicleId())) throw error("EVENT_RIDE_OFFER_INVALID");
        EventRegistration target = null;
        if (approve) {
            target = destination(event.getId(),guest.requestedVehicleId(),guest.originRegistrationId());
            guestSpace(event.getId(),guest.requestedVehicleId());
        }
        return change(guest,guest.review(approve,target == null ? null : target.id(),target == null ? null : target.userId(),actor,reason,now()),
            approve ? "APPROVED" : "REJECTED",actor,approve ? "ADMIN_APPROVED" : reason,null);
    }
    public EventRegistration destination(Long eventId,Long vehicleId,Long registrationId) {
        var vehicle = vehicleLocks.activeById(vehicleId).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
        return refreshed(eventId).stream().filter(r -> r.status() == EventRegistration.Status.CONFIRMED
            && r.userId().equals(vehicle.ownerId()) && (registrationId == null || r.id().equals(registrationId))
            && r.allocations().stream().anyMatch(a -> a.vehicleId().equals(vehicleId)))
            .findFirst().orElseThrow(() -> error("EVENT_PAYMENT_REQUIRED"));
    }
    public EventGuestRequest assign(Event event,Long guestId,Long vehicle,Long registration,Long offerId,Long actor,String reason) {
        requireOpen(event);
        var guest = guest(event.getId(),guestId);
        if (!guest.administrative() || guest.status() != EventGuestRequest.Status.APPROVED) throw error("EVENT_GUEST_TRANSITION_INVALID");
        var target = destination(event.getId(),vehicle,registration);
        if (Objects.equals(guest.vehicleId(),vehicle) && (offerId == null || offerId.equals(guest.selectedOfferId()))) return guest;
        if (!Objects.equals(guest.vehicleId(),vehicle)) guestSpace(event.getId(),vehicle);
        withdrawOffers(guest,offerId,actor,reason);
        return change(guest,guest.allocate(vehicle,target.id(),target.userId(),offerId),
            guest.vehicleId() == null ? "ALLOCATED" : "REALLOCATED",actor,reason,offerId);
    }
    public EventGuestRequest unassign(Event event,Long guestId,Long actor,String reason) {
        var guest = guest(event.getId(),guestId);
        if (!guest.administrative() || guest.status() != EventGuestRequest.Status.APPROVED) throw error("EVENT_GUEST_TRANSITION_INVALID");
        if (guest.vehicleId() == null) return guest;
        requireOpen(event);
        withdrawOffers(guest,null,actor,reason);
        return change(guest,guest.unassign(),"UNASSIGNED",actor,reason,guest.selectedOfferId());
    }
    public EventGuestRequest cancelGuest(Event event,Long guestId,Long actor,String reason,boolean administrator) {
        var guest = guest(event.getId(),guestId);
        if (!administrator && (guest.administrative() || !guest.requesterUserId().equals(actor))) throw error("EVENT_GUEST_ACCESS_DENIED");
        if (guest.status() == EventGuestRequest.Status.CANCELLED) return guest;
        requireOpen(event);
        if (guest.status() == EventGuestRequest.Status.REJECTED) throw error("EVENT_GUEST_TRANSITION_INVALID");
        withdrawOffers(guest,null,actor,reason);
        return change(guest,guest.cancel(),"CANCELLED",actor,reason,guest.selectedOfferId());
    }
    public EventGuestRequest reactivate(Event event,Long guestId,Long actor,String reason) {
        var guest = guest(event.getId(),guestId);
        if (!guest.administrative()) throw error("EVENT_GUEST_TRANSITION_INVALID");
        if (guest.status() == EventGuestRequest.Status.APPROVED && guest.vehicleId() == null) return guest;
        requireOpen(event);
        if (guest.status() != EventGuestRequest.Status.CANCELLED) throw error("EVENT_GUEST_TRANSITION_INVALID");
        return change(guest,guest.reactivate(),"REACTIVATED",actor,reason,null);
    }
    public EventRegistration cancelRegistration(Event event,EventRegistration registration,Long actor) {
        if (registration.status() == EventRegistration.Status.CANCELLED) return registration;
        requireOpen(event);
        for (var guest : operations.guests(event.getId())) {
            if (!guest.administrative() && Objects.equals(guest.originRegistrationId(),registration.id())
                    && (guest.status() == EventGuestRequest.Status.PENDING || guest.status() == EventGuestRequest.Status.APPROVED)) {
                cancelGuest(event,guest.id(),actor,"REGISTRATION_CANCELLED",true);
            } else if (guest.administrative() && Objects.equals(guest.allocatedRegistrationId(),registration.id())) {
                unassign(event,guest.id(),actor,"DRIVER_REGISTRATION_CANCELLED");
            }
        }
        for (var offer : operations.offers(event.getId())) {
            if (offer.registrationId().equals(registration.id()) && (offer.status() == EventRideOffer.Status.ACCEPTED || offer.status() == EventRideOffer.Status.SELECTED)) {
                operations.save(offer.withdraw(now()));
                var guest = guest(event.getId(),offer.guestRequestId());
                record(guest,guest,"OFFER_WITHDRAWN",actor,"DRIVER_REGISTRATION_CANCELLED",offer.id());
            }
        }
        return operations.save(registration.cancel(now()));
    }
    private void withdrawOffers(EventGuestRequest guest,Long keepOffer,Long actor,String reason) {
        for (var offer : operations.offers(guest.eventId())) {
            if (offer.guestRequestId().equals(guest.id()) && !Objects.equals(offer.id(),keepOffer)
                    && (offer.status() == EventRideOffer.Status.ACCEPTED || offer.status() == EventRideOffer.Status.SELECTED)) {
                operations.save(offer.withdraw(now()));
                record(guest,guest,"OFFER_WITHDRAWN",actor,reason,offer.id());
            }
        }
    }
    private EventGuestRequest change(EventGuestRequest before,EventGuestRequest after,String action,Long actor,String reason,Long offer) {
        var saved = operations.save(after);
        record(before,saved,action,actor,reason,offer);
        return saved;
    }
    public void record(EventGuestRequest before,EventGuestRequest after,String action,Long actor,String reason,Long offer) {
        operations.record(new EventGuestTransition(null,after.eventId(),after.id(),action,actor,reason,now(),
            before == null ? null : before.status().name(),after.status().name(),
            before == null ? null : before.vehicleId(),after.vehicleId(),
            before == null ? null : before.allocatedRegistrationId(),after.allocatedRegistrationId(),offer));
    }
    public static EventOperationException error(String code) { return new EventOperationException(code); }

    /** Fixed parent order for registration writes: sorted Vehicles, then all sorted Dependents. */
    public void lockReferences(Long user,List<EventRegistration.Allocation> allocations,List<Long> unallocated) {
        allocations.stream().map(EventRegistration.Allocation::vehicleId).distinct().sorted()
                .forEach(id -> {
                    var vehicle = vehicleLocks.activeById(id).orElseThrow(() -> error("EVENT_VEHICLE_NOT_FOUND"));
                    if(!vehicle.ownerId().equals(user)) throw error("EVENT_VEHICLE_NOT_OWNED");
                });
        java.util.stream.Stream.concat(unallocated.stream(),allocations.stream().flatMap(a -> a.dependentIds().stream()))
                .distinct().sorted().forEach(id -> { if(!activeDependent(id,user)) throw error("EVENT_PARTICIPANT_NOT_FOUND"); });
    }
    private boolean activeDependent(Long id,Long user) {
        return dependentLocks.byId(id).filter(d -> d.active() && d.userId().equals(user)).isPresent();
    }
}
