package com.jeepclub.backend.publications.infra.persistence.query;
import com.jeepclub.backend.publications.core.repository.EventReportVolumeQuery;
import com.jeepclub.backend.shared.export.*;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class EventReportVolumeJpaQuery implements EventReportVolumeQuery {
    private final jakarta.persistence.EntityManager em;
    public void requireWithinLimit(Long id) {
        long count=0;
        for(String entity:java.util.List.of("EventRegistrationEntity","EventGuestRequestEntity","EventRideOfferEntity","EventChargeRuleEntity"))
            count+=em.createQuery("select count(e) from "+entity+" e where e.eventId=:id",Long.class).setParameter("id",id).getSingleResult();
        for(String collection:java.util.List.of("occupants","vehicleIds","unallocatedDependentIds"))
            count+=em.createQuery("select count(e.id) from EventRegistrationEntity e join e."+collection+" c where e.eventId=:id",Long.class).setParameter("id",id).getSingleResult();
        if(count>2000)throw new ExportException(ExportException.Reason.LIMIT);
    }
}
