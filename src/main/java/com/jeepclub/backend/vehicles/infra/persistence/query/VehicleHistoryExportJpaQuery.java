package com.jeepclub.backend.vehicles.infra.persistence.query;
import com.jeepclub.backend.vehicles.core.repository.VehicleHistoryExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class VehicleHistoryExportJpaQuery implements VehicleHistoryExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.nickname, case when e.photo is not null then true else false end, e.plate, e.renavam, e.brand, e.model, e.manufacturingYear, e.modelYear, e.color, e.seatingCapacity, e.fuelType, e.engineDisplacement, e.status, e.towing, e.ownerId, e.createdAt, e.updatedAt, e.vehicleId, e.deletedByUserId, e.deletedAt from VehicleHistoryEntity e where (:id is null or e.id = :id) order by e.id", Object[].class)
            .setParameter("id", id).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10], t[11], t[12], t[13], t[14], t[15], t[16], t[17], t[18], t[19], t[20]))).toList();
    }
}
