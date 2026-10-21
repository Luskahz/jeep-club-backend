package com.jeepclub.backend.billing.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class BillingUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM billing_charge_cycles WHERE (archived_by_user_id = :id OR canceled_by_user_id = :id OR finished_by_user_id = :id OR generated_by_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM billing_member_charges WHERE (user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM billing_member_payments WHERE (confirmed_by_user_id = :id OR rejected_by_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM billing_member_refunds WHERE (approved_by_user_id = :id OR canceled_by_user_id = :id OR created_by_user_id = :id OR refunded_by_user_id = :id OR rejected_by_user_id = :id OR requested_by_user_id = :id OR user_id = :id) LIMIT 1 FOR UPDATE","SELECT charge_assignment_id FROM billing_user_charge_assignments WHERE (user_id = :id) LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
