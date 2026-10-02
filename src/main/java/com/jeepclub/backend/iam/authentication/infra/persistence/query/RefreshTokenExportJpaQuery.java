package com.jeepclub.backend.iam.authentication.infra.persistence.query;
import com.jeepclub.backend.iam.authentication.core.repository.RefreshTokenExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class RefreshTokenExportJpaQuery implements RefreshTokenExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.id, e.sessionId, e.createdAt, e.expiresAt, e.status, e.replacedByTokenId from RefreshTokenEntity e where (:id is null or e.id = :id) and (:status is null or cast(e.status as string) = :status) and (:ownerId is null or exists (select s.id from SessionEntity s where s.id=e.sessionId and s.userId=:ownerId)) order by e.id", Object[].class)
            .setParameter("id", id).setParameter("ownerId",ownerId)
            .setParameter("status", status).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5]))).toList();
    }
}
