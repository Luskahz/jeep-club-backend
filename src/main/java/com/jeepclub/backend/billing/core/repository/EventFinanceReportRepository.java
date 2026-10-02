package com.jeepclub.backend.billing.core.repository;
import com.jeepclub.backend.billing.api.module.EventFinanceReportQuery.Definition;
public interface EventFinanceReportRepository {
 java.util.List<Definition> definitions(java.util.Collection<Long> ids);
 java.util.Map<String,Long> paymentCounts(Long eventId);
 void requireWithinLimit(Long eventId);
}
