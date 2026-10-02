package com.jeepclub.backend.billing.core.application.query;
import com.jeepclub.backend.billing.api.module.EventFinanceReportQuery;
import com.jeepclub.backend.billing.core.repository.EventFinanceReportRepository;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly=true)
public class EventFinanceReportQueryService implements EventFinanceReportQuery {
    private final EventFinanceReportRepository repository;
    public java.util.List<Definition> definitions(java.util.Collection<Long> ids) {
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");return repository.definitions(ids);
    }
    public java.util.Map<String,Long> paymentCounts(Long id){return repository.paymentCounts(id);}
    public void requireWithinLimit(Long id){repository.requireWithinLimit(id);}
}
