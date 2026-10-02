package com.jeepclub.backend.billing.core.repository;
import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.shared.export.ExportRow;
public interface BillingExportQuery {
    java.util.Map<Long,com.jeepclub.backend.shared.export.ExportRow> latestPayments(java.util.Collection<Long> chargeIds);
    enum Product { DEFINITIONS,CYCLES,CHARGES,PAYMENTS,REFUNDS }
    record Entry(Long userId,Long roleId,Long eventId,MemberCharge charge,ExportRow row) {}
    java.util.List<Entry> read(Product product,BillingExportFilter filter,int offset);
    boolean exists(Product product,Long id);
    boolean hasEventContext(Long eventId);
}
