package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.*;

public record EventRegistration(Long id, Long eventId, Long userId, Status status,
        List<Allocation> allocations, Instant createdAt, Instant confirmedAt, Instant cancelledAt, List<Long> unallocatedDependentIds) {
    public EventRegistration(Long id, Long eventId, Long userId, Status status, List<Allocation> allocations,
            Instant createdAt, Instant confirmedAt, Instant cancelledAt) {
        this(id, eventId, userId, status, allocations, createdAt, confirmedAt, cancelledAt, List.of());
    }
    public enum Status { PENDING_PAYMENT, CONFIRMED, CANCELLED }
    public record Allocation(Long vehicleId, boolean member, List<Long> dependentIds) {
        public Allocation {
            Publication.positive(vehicleId, "vehicleId");
            dependentIds = List.copyOf(dependentIds);
        }
    }
    public EventRegistration {
        Publication.positive(eventId, "eventId"); Publication.positive(userId, "userId");
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
        allocations = List.copyOf(allocations);
        unallocatedDependentIds = List.copyOf(unallocatedDependentIds);
        var vehicles = new HashSet<Long>(); var dependents = new HashSet<Long>();
        for (Long dependent : unallocatedDependentIds) {
            Publication.positive(dependent, "dependentId");
            if (!dependents.add(dependent)) throw new IllegalArgumentException("Duplicate dependent.");
        }
        int members = 0;
        for (var allocation : allocations) {
            if (!vehicles.add(allocation.vehicleId())) throw new IllegalArgumentException("Duplicate vehicle.");
            if (allocation.member()) members++;
            for (Long dependent : allocation.dependentIds()) {
                Publication.positive(dependent, "dependentId");
                if (!dependents.add(dependent)) throw new IllegalArgumentException("Duplicate dependent.");
            }
        }
        if (members > 1) throw new IllegalArgumentException("Member cannot occupy multiple vehicles.");
        if (!allocations.isEmpty() && members != 1) throw new IllegalArgumentException("Allocate the member to one vehicle.");
    }
    public EventRegistration confirm(Instant now) {
        if (status != Status.PENDING_PAYMENT) return this;
        return new EventRegistration(id, eventId, userId, Status.CONFIRMED, allocations, createdAt, now, null, unallocatedDependentIds);
    }
    public EventRegistration cancel(Instant now) {
        if (status == Status.CANCELLED) throw new IllegalStateException("Registration already cancelled.");
        return new EventRegistration(id, eventId, userId, Status.CANCELLED, allocations, createdAt, confirmedAt, now, unallocatedDependentIds);
    }
    public EventRegistration identified(Long value) {
        return new EventRegistration(value, eventId, userId, status, allocations, createdAt, confirmedAt, cancelledAt, unallocatedDependentIds);
    }
    public EventRegistration allocate(List<Allocation> replacement, List<Long> unallocated) {
        if (status == Status.CANCELLED) throw new IllegalStateException("Cancelled registration cannot be edited.");
        return new EventRegistration(id, eventId, userId, status, replacement, createdAt, confirmedAt, cancelledAt, unallocated);
    }
}
