package com.jeepclub.backend.iam.authorization.infra.integration.identity;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionCleanup;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.*;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.UserRoleRemovalHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
@Component @RequiredArgsConstructor
public class AuthorizationUserDeletionCleanup implements UserDeletionCleanup {
    private final UserRoleJpaRepository bindings;
    private final UserRoleRemovalHistoryJpaRepository history;
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void removePendingAssociations(Long userId, Long actorId, Instant now) {
        var rows=bindings.findByUserIdOrderById(userId);
        for(var row:rows) {
            if(row.getRole().getKind()==com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind.ROOT)
                throw new com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException("ROOT user is protected from deletion.");
            history.save(new UserRoleRemovalHistoryEntity(row.getId(),row.getRole().getId(),row.getRole().getName(),
                    userId,row.getCreatedAt(),actorId,now,"PENDING_IDENTITY_DELETED",1));
        }
        history.flush();
        bindings.deleteAll(rows);
        bindings.flush();
    }
}
