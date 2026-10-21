package com.jeepclub.backend.tools.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class ToolsUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM tools WHERE (user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM tools_tool_history WHERE (user_id = :id OR deleted_by_user_id = :id) LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
