package com.jeepclub.backend.iam.authorization.infra.persistence.query;
import com.jeepclub.backend.iam.authorization.core.repository.RoleExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class RoleExportJpaQuery implements RoleExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.name, e.description, e.kind, e.status, e.createdAt, e.updatedAt, e.deletedAt from RoleEntity e where (:id is null or e.id = :id) and (:name is null or lower(e.name) like :name) and (:status is null or cast(e.status as string) = :status) order by e.id", Object[].class)
            .setParameter("id", id)
            .setParameter("name", name == null || name.isBlank() ? null : "%" + name.toLowerCase(java.util.Locale.ROOT) + "%")
            .setParameter("status", status).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]))).toList();
    }
}
