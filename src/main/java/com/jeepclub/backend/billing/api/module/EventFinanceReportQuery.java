package com.jeepclub.backend.billing.api.module;
import java.util.*;
import java.math.BigDecimal;
public interface EventFinanceReportQuery {
    record Definition(Long id,String name,BigDecimal amount) {}
    List<Definition> definitions(Collection<Long> ids);
    Map<String,Long> paymentCounts(Long eventId);
    void requireWithinLimit(Long eventId);
}
