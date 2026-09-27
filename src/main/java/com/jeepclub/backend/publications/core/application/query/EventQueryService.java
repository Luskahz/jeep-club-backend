package com.jeepclub.backend.publications.core.application.query;
import com.jeepclub.backend.publications.api.module.EventQuery;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class EventQueryService implements EventQuery {
    private final PublicationRepository publications;
    private final EventOperationsRepository operations;
    public boolean exists(Long id) { return publications.findById(id).filter(Event.class::isInstance).isPresent(); }
    public List<Long> confirmedParticipants(Long id) {
        return operations.registrations(id).stream().filter(r -> r.status() == EventRegistration.Status.CONFIRMED).map(EventRegistration::userId).toList();
    }
}
