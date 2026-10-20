package com.jeepclub.backend.memberships.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class MembershipsUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM membership_members WHERE (identity_user_id = :id) LIMIT 1 FOR UPDATE","SELECT claim_key FROM membership_number_claims WHERE (actor_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM membership_member_audits WHERE (actor_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM membership_applications WHERE (created_user_id = :id AND status IN ('APPROVED','COMPLETED')) OR reviewed_by_user_id = :id LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
