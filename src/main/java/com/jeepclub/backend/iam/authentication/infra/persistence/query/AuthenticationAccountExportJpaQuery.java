package com.jeepclub.backend.iam.authentication.infra.persistence.query;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountExportQuery;
import com.jeepclub.backend.shared.export.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository @RequiredArgsConstructor
public class AuthenticationAccountExportJpaQuery implements AuthenticationAccountExportQuery {
    private final EntityManager em;
    public List<Entry> read(Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to, int offset) {
        return em.createQuery("select e.identityId, e.accessStatus, e.authenticationStatus, e.credentialStatus, e.lastLoginAt, e.createdAt, e.accessDisabledAt, e.updatedAt, e.passwordChangedAt, e.failedLoginAttempts from AuthenticationAccountEntity e where (:id is null or e.identityId = :id) order by e.identityId", Object[].class)
            .setParameter("id", id).setFirstResult(offset).setMaxResults(ExportPages.CHUNK)
            .getResultList().stream().map(t -> new Entry(null, ExportRow.of(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9]))).toList();
    }
}
