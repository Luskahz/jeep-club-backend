package com.jeepclub.backend.dependents.core.repository;
import java.util.List;
import com.jeepclub.backend.shared.export.ExportRow;
public interface DependentHistoryExportQuery {
    record Entry(Long ownerId, ExportRow row) {}
    List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset);
}
