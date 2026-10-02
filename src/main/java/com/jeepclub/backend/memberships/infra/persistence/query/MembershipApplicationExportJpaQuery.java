package com.jeepclub.backend.memberships.infra.persistence.query;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class MembershipApplicationExportJpaQuery implements MembershipApplicationExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.name, e.cpf, e.email, e.phoneNumber, e.message, e.status, e.rejectionReason, e.reviewedByUserId, e.createdUserId, e.requestedAt, e.reviewedAt, e.finishedAt, e.updatedAt from MembershipApplicationEntity e where (:id is null or e.id = :id) and (:name is null or lower(e.name) like :name) and (:status is null or cast(e.status as string) = :status) and (:from is null or e.requestedAt >= :from) and (:to is null or e.requestedAt <= :to) order by e.id", Object[].class)
            .setParameter("id", id)
            .setParameter("name", name == null || name.isBlank() ? null : "%" + name.toLowerCase(java.util.Locale.ROOT) + "%")
            .setParameter("status", status)
            .setParameter("from", from).setParameter("to", to).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10], t[11], t[12], t[13]))).toList();
    }
}
