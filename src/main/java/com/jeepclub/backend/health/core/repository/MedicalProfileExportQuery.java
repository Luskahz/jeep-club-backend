package com.jeepclub.backend.health.core.repository;
import com.jeepclub.backend.shared.export.ExportRow;
import java.util.*;
public interface MedicalProfileExportQuery {
    record Entry(String ownerType,Long ownerId,ExportRow row) {}
    List<Entry> read(boolean history,Long id,String ownerType,Collection<Long> ownerIds,int offset);
}
