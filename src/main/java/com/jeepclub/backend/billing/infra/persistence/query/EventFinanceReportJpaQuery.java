package com.jeepclub.backend.billing.infra.persistence.query;
import com.jeepclub.backend.billing.core.repository.EventFinanceReportRepository;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.billing.api.module.EventFinanceReportQuery.Definition;
import java.util.*;
import java.math.BigDecimal;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class EventFinanceReportJpaQuery implements EventFinanceReportRepository {
    private final jakarta.persistence.EntityManager em;
    public List<Definition> definitions(Collection<Long> ids) {
        if(ids.isEmpty())return List.of();
        return em.createQuery("select d.id,d.name,d.defaultAmount from ChargeDefinitionEntity d where d.id in :ids",Object[].class)
            .setParameter("ids",ids).getResultList().stream().map(t->new Definition((Long)t[0],(String)t[1],(BigDecimal)t[2])).toList();
    }
    public Map<String,Long> paymentCounts(Long id) {
        var rows=em.createQuery("select p.status,count(p.id) from MemberPaymentEntity p join MemberChargeEntity c on c.id=p.memberChargeId join EventChargeContextEntity x on x.cycleId=c.chargeCycleId where x.eventId=:id group by p.status",Object[].class).setParameter("id",id).getResultList();
        var result=new java.util.TreeMap<String,Long>();rows.forEach(t->result.put(t[0].toString(),(Long)t[1]));
        for(var status:com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.values())result.putIfAbsent(status.name(),0L);
        return result;
    }
    public void requireWithinLimit(Long id) {
        long charges=em.createQuery("select count(c.id) from MemberChargeEntity c join EventChargeContextEntity x on x.cycleId=c.chargeCycleId where x.eventId=:id",Long.class).setParameter("id",id).getSingleResult();
        long payments=em.createQuery("select count(p.id) from MemberPaymentEntity p join MemberChargeEntity c on c.id=p.memberChargeId join EventChargeContextEntity x on x.cycleId=c.chargeCycleId where x.eventId=:id",Long.class).setParameter("id",id).getSingleResult();
        if(charges+payments>20000)throw new ExportException(ExportException.Reason.LIMIT);
    }
}
