package com.jeepclub.backend.iam.authorization.infra.persistence.query;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class PermissionExportJpaQuery implements PermissionExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.code, e.description, e.module, e.createdAt, e.updatedAt from PermissionEntity e where (:id is null or e.id = :id) order by e.id", Object[].class)
            .setParameter("id", id).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5]))).toList();
    }
}
