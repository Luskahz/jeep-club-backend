package com.jeepclub.backend.memberships.infra.persistence.query;
import com.jeepclub.backend.memberships.core.repository.ApplicantBlockExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class ApplicantBlockExportJpaQuery implements ApplicantBlockExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.cpf, e.reason, e.blockedAt, e.blockedByUserId, e.unblockedAt, e.unblockedByUserId from MembershipApplicantBlockEntity e where (:id is null or e.id = :id) and (:from is null or e.blockedAt >= :from) and (:to is null or e.blockedAt <= :to) order by e.id", Object[].class)
            .setParameter("id", id)
            .setParameter("from", from).setParameter("to", to).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5], t[6]))).toList();
    }
}
