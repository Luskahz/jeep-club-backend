package com.jeepclub.backend.health.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class HealthUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM medical_profiles WHERE (owner_type = 'USER' AND (owner_id = :id)) LIMIT 1 FOR UPDATE","SELECT id FROM medical_profiles_history WHERE ((owner_type = 'USER' AND owner_id = :id) OR deleted_by_user_id = :id) LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
