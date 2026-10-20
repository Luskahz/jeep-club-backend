package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.*;
import java.util.*;

/** Rows survive publication hard delete; scalar IDs deliberately have no destructive cascade. */
public interface EventOperationsRepository {
    record References(List<Long> vehicleIds,List<Long> dependentIds) {}
    References committedReferences(Long eventId);
    List<EventChargeRule> rules(Long eventId);
    void replaceRules(Long eventId, List<EventChargeRule> rules);
    List<EventRegistration> registrations(Long eventId);
    EventRegistration save(EventRegistration registration);
    List<EventGuestRequest> guests(Long eventId);
    EventGuestRequest save(EventGuestRequest request);
    long approvedVisits(String cpf);
    java.util.Map<String, Long> approvedVisits(java.util.Collection<String> cpfs);
    List<EventRideOffer> offers(Long eventId);
    EventRideOffer save(EventRideOffer offer);
    void record(EventGuestTransition transition);
    List<EventGuestTransition> history(Long eventId, Long guestRequestId);
}
