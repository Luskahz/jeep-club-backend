package com.jeepclub.backend.publications.core.repository;
import com.jeepclub.backend.shared.export.ExportRow;
import java.time.Instant;
public interface PublicationExportQuery {
    java.util.Map<Long,String> ruleSummaries(java.util.Collection<Long> eventIds);
    enum Product {NOTICES,NOTICE_HISTORY,SERVICES,SERVICE_HISTORY,EVENTS,EVENT_HISTORY,SERVICE_REQUESTS,CHANGE_REQUESTS}
    record Entry(Long authorId,ExportRow row) {}
    java.util.List<Entry> read(Product p,Long id,String status,String lifecycle,Instant from,Instant to,Instant now,int offset);
}
