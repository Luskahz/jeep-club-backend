package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.application.service.internal.EventOperations;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.repository.EventOperationsRepository;
import com.jeepclub.backend.billing.api.module.EventFinancialQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.jeepclub.backend.publications.core.application.service.internal.EventOperations.error;

@Service @RequiredArgsConstructor @Transactional
public class EventService {
    private final EventOperations events;
    private final EventOperationsRepository operations;
    private final EventFinancialQuery financial;
    public Event findById(Long id) {
        var event = events.locked(id);
        if (event.getStatus() != PublicationStatus.PUBLISHED) throw error("EVENT_NOT_FOUND");
        return event;
    }
    public EventRegistration register(Long id, Long user, List<EventRegistration.Allocation> allocations) {
        return register(id,user,allocations,List.of());
    }
    public EventRegistration register(Long id, Long user, List<EventRegistration.Allocation> allocations, List<Long> dependentIds) {
        return events.register(events.locked(id,allocations.stream().map(EventRegistration.Allocation::vehicleId).toList(),
            java.util.stream.Stream.concat(dependentIds.stream(),allocations.stream().flatMap(a -> a.dependentIds().stream())).toList()), user, allocations,dependentIds);
    }
    public EventRegistration mine(Long id, Long user) {
        events.locked(id);
        return events.refresh(events.registration(id, user), financial.findByEvent(id));
    }
    public EventRegistration cancel(Long id, Long user) {
        var event = events.locked(id);
        var registration = events.registration(id, user);
        return events.cancelRegistration(event,registration,user);
    }
    public EventRegistration allocate(Long id, Long user, List<EventRegistration.Allocation> allocations, List<Long> unallocated) {
        var event = events.locked(id,allocations.stream().map(EventRegistration.Allocation::vehicleId).toList(),
            java.util.stream.Stream.concat(unallocated.stream(),allocations.stream().flatMap(a -> a.dependentIds().stream())).toList()); events.requireOpen(event);
        var registration = events.registration(id, user);
        var replacement = registration.allocate(allocations, unallocated);
        events.lockReferences(user,allocations,unallocated);
        events.validateDependents(user, unallocated);
        events.validateAllocations(user, allocations);
        var approved = operations.guests(id).stream().filter(g -> g.status() == EventGuestRequest.Status.APPROVED).toList();
        for (var previous : registration.allocations()) {
            boolean guest = approved.stream().anyMatch(g -> previous.vehicleId().equals(g.vehicleId()));
            if (guest && allocations.stream().noneMatch(a -> a.vehicleId().equals(previous.vehicleId()))) throw error("EVENT_INVALID_STATE");
        }
        operations.save(replacement);
        for (var allocation : allocations) if (events.spare(id, allocation.vehicleId()) < 0) throw error("EVENT_VEHICLE_CAPACITY_EXCEEDED");
        return replacement;
    }
    public EventGuestRequest requestGuest(Long id, Long user, Long vehicle, String cpf, String guestName) {
        if(guestName==null || guestName.isBlank() || guestName.trim().length()>150)throw new IllegalArgumentException("Guest name required.");
        return events.requestGuest(events.locked(id,vehicle == null ? List.of() : List.of(vehicle),List.of()),user,vehicle,cpf,false,guestName);
    }
    public EventGuestRequest requestGuest(Long id, Long user, Long vehicle, String cpf) {
        return events.requestGuest(events.locked(id,vehicle == null ? List.of() : List.of(vehicle),List.of()), user, vehicle, cpf, false);
    }
    public EventGuestRequest cancelGuest(Long id,Long guestId,Long user,String reason) {
        return events.cancelGuest(events.locked(id),guestId,user,reason,false);
    }
    public List<EventGuestRequest> guests(Long id, Long user) {
        events.locked(id);
        return operations.guests(id).stream().filter(g -> !g.administrative() && g.requesterUserId().equals(user)).toList();
    }
    public List<Long> rideRequests(Long id, Long user) {
        var event = events.locked(id); events.requireOpen(event);
        var registration = events.registration(id, user);
        if (registration.status() != EventRegistration.Status.CONFIRMED) return List.of();
        boolean space = registration.allocations().stream().anyMatch(a -> events.spare(id, a.vehicleId()) > 0);
        return space ? operations.guests(id).stream().filter(g -> g.administrative() && g.transportStatus() == EventGuestRequest.TransportStatus.STANDBY).map(EventGuestRequest::id).toList() : List.of();
    }
    public EventRideOffer respond(Long id, Long guestId, Long user, Long vehicle, boolean accept) {
        var event = events.locked(id,List.of(vehicle),List.of()); events.requireOpen(event);
        if (!rideRequests(id, user).contains(guestId)) throw error("EVENT_RIDE_OFFER_INVALID");
        var registration = events.registration(id, user);
        if (registration.allocations().stream().noneMatch(a -> a.vehicleId().equals(vehicle))) throw error("EVENT_VEHICLE_NOT_OWNED");
        events.guestSpace(id, vehicle);
        var previous = operations.offers(id).stream().filter(o -> o.guestRequestId().equals(guestId) && o.vehicleId().equals(vehicle)
            && (o.status() == EventRideOffer.Status.ACCEPTED || o.status() == EventRideOffer.Status.SELECTED)).findFirst();
        if (previous.isPresent()) throw error("EVENT_RIDE_OFFER_INVALID");
        var offer = operations.save(new EventRideOffer(null, id, guestId, registration.id(), user, vehicle,
            accept ? EventRideOffer.Status.ACCEPTED : EventRideOffer.Status.DECLINED, events.now(), null));
        var guest = events.guest(id,guestId);
        events.record(guest,guest,accept ? "OFFER_ACCEPTED" : "OFFER_DECLINED",user,"MEMBER_RIDE_RESPONSE",offer.id());
        return offer;
    }
}
