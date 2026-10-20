package com.jeepclub.backend.vehicles.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class VehiclesUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM vehicles_vehicle WHERE (owner_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM vehicles_vehicle_history WHERE (owner_id = :id OR deleted_by_user_id = :id) LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
