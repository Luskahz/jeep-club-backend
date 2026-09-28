package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.application.service.internal.EventOperations;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.*;
import com.jeepclub.backend.publications.core.repository.*;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.platform.logging.*;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.jeepclub.backend.publications.core.application.service.internal.EventOperations.error;

@Service @RequiredArgsConstructor @Transactional
public class AdminEventService {
    private final EventOperations events;
    private final PublicationRepository publications;
    private final EventOperationsRepository operations;
    private final ImageMediaService media;
    private final EventBillingCommand billing;
    private final EventFinancialQuery financial;
    private final EventChargeCatalogQuery catalog;
    private final EventVehicleQuery vehicles;
    private final EmergencyMedicalProfileQuery health;
    private final DependentsQuery dependents;
    private final SystemLogService audit;

    public record ChargeConfiguration(Long chargeDefinitionId, String name, String description, BigDecimal amount,
        boolean billingRequired, boolean requiredForParticipation, Instant participationCutoff, LocalDate financialDueDate) {
        public ChargeConfiguration(Long chargeDefinitionId, String name, String description, BigDecimal amount,
                boolean billingRequired, boolean requiredForParticipation, Instant participationCutoff) {
            this(chargeDefinitionId, name, description, amount, billingRequired, requiredForParticipation, participationCutoff, null);
        }
    }
    public Event create(Long actor, String title, String content, List<PublicationImage> images,
                        Instant startsAt, Instant endsAt, List<ChargeConfiguration> charges) {
        if (startsAt == null || !startsAt.isAfter(events.now())) throw new IllegalArgumentException("Future startsAt required.");
        var event = Event.create(actor, title, content, images, startsAt, events.now());
        event.schedule(startsAt, endsAt, events.now());
        images.forEach(i -> media.requireExisting(i.storageKey()));
        event = (Event) publications.save(event);
        configure(event, charges == null ? List.of() : charges);
        return event;
    }
    public Event findById(Long id) { return events.locked(id); }
    @Transactional(readOnly=true)
    public Page<Event> findAll(Pageable pageable) { return publications.findEvents(pageable); }
    public List<EventChargeRule> rules(Long id) { events.locked(id); return operations.rules(id); }
    public Page<EventChargeCatalogQuery.Entry> catalog(Pageable pageable) { return catalog.findEligible(pageable); }
    public Event update(Long id, String title, String content, List<PublicationImage> images, Instant startsAt,
            boolean endsPresent, Instant endsAt, List<ChargeConfiguration> charges) {
        var event = events.locked(id);
        if (charges != null || startsAt != null || endsPresent) {
            events.requireOpen(event);
            if (!operations.registrations(id).isEmpty() || !financial.findByEvent(id).isEmpty()) throw error("EVENT_CHARGE_CONFIGURATION_LOCKED");
        }
        if (startsAt != null || endsPresent) event.schedule(startsAt == null ? event.getStartsAt() : startsAt, endsPresent ? endsAt : event.getEndsAt(), events.now());
        if (charges == null && operations.rules(id).stream().anyMatch(r -> r.participationCutoff().isAfter(event.getStartsAt()))) throw error("EVENT_CHARGE_INVALID");
        if (title != null || content != null) event.updateContent(title == null ? event.getTitle() : title, content == null ? event.getContent() : content, events.now());
        if (images != null) {
            images.forEach(i -> media.requireExisting(i.storageKey()));
            event.replaceImages(images, events.now());
        }
        if (charges != null) configure(event, charges);
        return (Event) publications.save(event);
    }
    private void configure(Event event, List<ChargeConfiguration> configurations) {
        var rules = new ArrayList<EventChargeRule>();
        var ids = new HashSet<Long>();
        for (var c : configurations) {
            if (c.chargeDefinitionId() != null && (c.name() != null || c.amount() != null)) throw error("EVENT_CHARGE_INVALID");
            Long id = c.chargeDefinitionId() != null ? c.chargeDefinitionId()
                : billing.createOneTimeEventCharge(event.getId(), c.name(), c.description(), c.amount(), c.billingRequired());
            if (!ids.add(id)) throw error("EVENT_CHARGE_INVALID");
            if ((c.requiredForParticipation() || c.financialDueDate() == null)
                    && !"AFTER_DUE_DATE".equals(catalog.getEligible(id).paymentAcceptancePolicy()))
                throw error("EVENT_CHARGE_INVALID");
            billing.ensureAssignment(event.getId(), id);
            Instant cutoff = c.participationCutoff() == null ? event.getStartsAt() : c.participationCutoff();
            if (cutoff.isAfter(event.getStartsAt()) || !cutoff.isAfter(events.now())) throw error("EVENT_CHARGE_INVALID");
            rules.add(new EventChargeRule(event.getId(), id, c.requiredForParticipation(), cutoff, c.financialDueDate()));
        }
        operations.replaceRules(event.getId(), rules);
    }
    public Event publish(Long id) { var e = events.locked(id); e.publish(events.now()); return (Event) publications.save(e); }
    public Event finish(Long id) { var e = events.locked(id); events.refreshed(id); e.finish(events.now()); return (Event) publications.save(e); }
    public Event cancel(Long id, Long actor) {
        var e = events.locked(id); e.cancel(events.now()); billing.cancelEventCycles(id, actor);
        return (Event) publications.save(e);
    }
    public void delete(Long id, Long actor) {
        var e = events.locked(id);
        if (!operations.registrations(id).isEmpty() && e.effectiveStatus(events.now()) != EventStatus.CANCELLED && e.effectiveStatus(events.now()) != EventStatus.FINISHED)
            throw error("EVENT_INVALID_STATE");
        publications.delete(id, actor, events.now());
    }
    public List<EventGuestRequest> guests(Long id) { events.locked(id); return operations.guests(id); }
    public long visits(String cpf) { return operations.approvedVisits(cpf); }
    public Map<String,Long> visits(Collection<String> cpfs) { return operations.approvedVisits(cpfs); }
    public EventGuestRequest createGuest(Long id, Long actor, String cpf, String guestName) {
        if(guestName==null || guestName.isBlank() || guestName.trim().length()>150)throw new IllegalArgumentException("Guest name required.");
        return events.requestGuest(events.locked(id),actor,null,cpf,true,guestName);
    }
    public EventGuestRequest createGuest(Long id, Long actor, String cpf) { return events.requestGuest(events.locked(id), actor, null, cpf, true); }
    public EventGuestRequest reviewGuest(Long id, Long guest, Long actor, boolean approve, String reason) {
        return events.reviewGuest(events.locked(id), guest, approve, null, actor, reason);
    }
    public List<EventRideOffer> offers(Long id) { events.locked(id); return operations.offers(id); }
    public EventRideOffer select(Long id, Long offerId, Long actor) {
        var event = events.locked(id); events.requireOpen(event);
        var offer = operations.offers(id).stream().filter(o -> o.id().equals(offerId)).findFirst().orElseThrow(() -> error("EVENT_RIDE_OFFER_NOT_FOUND"));
        if (offer.status() != EventRideOffer.Status.ACCEPTED) throw error("EVENT_RIDE_OFFER_INVALID");
        try { events.guestSpace(id, offer.vehicleId()); }
        catch (com.jeepclub.backend.publications.core.application.exception.EventOperationException ex) { throw error("EVENT_RIDE_CAPACITY_CHANGED"); }
        events.reviewGuest(event, offer.guestRequestId(), true, offer.vehicleId(), actor, null);
        return operations.save(offer.select(events.now()));
    }
    public record Dashboard(List<EventRegistration> registrations, long dependents, long guestsPending, long guestsApproved,
        long totalPeople, int vehicles, int totalCapacity, int availableSeats, long unpaidCharges, long pendingValidation,
        long postCutoffPending, List<EventFinancialQuery.State> financial) {}
    public Dashboard dashboard(Long id) {
        events.locked(id);
        var registrations = events.refreshed(id);
        var active = registrations.stream().filter(r -> r.status() != EventRegistration.Status.CANCELLED).toList();
        var confirmed = registrations.stream().filter(r -> r.status() == EventRegistration.Status.CONFIRMED).toList();
        long dependentCount = confirmed.stream().flatMap(r -> r.allocations().stream()).mapToLong(a -> a.dependentIds().size()).sum()
            + confirmed.stream().mapToLong(r -> r.unallocatedDependentIds().size()).sum();
        var guests = operations.guests(id);
        long approved = guests.stream().filter(g -> g.status() == EventGuestRequest.Status.APPROVED).count();
        var vehicleIds = active.stream().flatMap(r -> r.allocations().stream()).map(EventRegistration.Allocation::vehicleId).distinct().toList();
        int capacity = vehicles.findActiveBatch(vehicleIds).stream().mapToInt(EventVehicleQuery.VehicleCapacity::seatingCapacity).sum();
        int reserved = active.stream().flatMap(r -> r.allocations().stream()).mapToInt(a -> a.dependentIds().size() + (a.member() ? 1 : 0)).sum();
        var states = financial.findByEvent(id);
        long unpaid = states.stream().filter(s -> !"PAID".equals(s.effectiveStatus()) && !"CANCELED".equals(s.effectiveStatus())).count();
        var rules = operations.rules(id);
        long late = states.stream().filter(s -> !"PAID".equals(s.effectiveStatus()) && !"CANCELED".equals(s.effectiveStatus())
            && rules.stream().anyMatch(r -> r.chargeDefinitionId().equals(s.chargeDefinitionId()) && !events.now().isBefore(r.participationCutoff()))).count();
        return new Dashboard(registrations, dependentCount, guests.stream().filter(g -> g.status() == EventGuestRequest.Status.PENDING).count(),
            approved, confirmed.size() + dependentCount + approved, vehicleIds.size(), capacity, Math.max(0, capacity - reserved - (int) approved),
            unpaid, states.stream().filter(s -> "PENDING_VALIDATION".equals(s.paymentStatus())).count(), late, states);
    }
    public EmergencyMedicalProfileQuery.Profile health(Long id, MedicalProfileOwner type, Long target, Long actor) {
        boolean success = false;
        try {
            var profile = readHealth(id,type,target);
            success = true;
            return profile;
        } finally {
            audit.recordRequired(new SystemLogEvent(actor, "EVENT_HEALTH_EMERGENCY_READ", "GET", "/admin/events/" + id + "/health/" + type + "/" + target,
                success ? 200 : 403, success ? SystemLogOutcome.SUCCESS : SystemLogOutcome.CLIENT_ERROR, 0, null, events.now()));
        }
    }
    private EmergencyMedicalProfileQuery.Profile readHealth(Long id, MedicalProfileOwner type, Long target) {
        var event = events.locked(id);
        // Temporal lifecycle is context only: a real emergency must remain accessible.
        var registrations = events.refreshed(id).stream().filter(r -> r.status() == EventRegistration.Status.CONFIRMED).toList();
        boolean participant = type == MedicalProfileOwner.USER ? registrations.stream().anyMatch(r -> r.userId().equals(target))
            : registrations.stream().anyMatch(r -> (r.unallocatedDependentIds().contains(target) || r.allocations().stream().anyMatch(a -> a.dependentIds().contains(target))) && dependents.isActiveDependentOfUser(target, r.userId()));
        if (!participant) throw error("EVENT_PARTICIPANT_NOT_FOUND");
        return health.find(type, target).orElseThrow(() -> error("EVENT_HEALTH_PROFILE_NOT_FOUND"));
    }
}
