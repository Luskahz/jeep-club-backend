package com.jeepclub.backend.iam.authorization.infra.persistence.query;
import com.jeepclub.backend.iam.authorization.core.repository.RoleDeletionHistoryQueryRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.*;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class RoleDeletionHistoryJpaQuery implements RoleDeletionHistoryQueryRepository {
    private final RoleDeletionHistoryJpaRepository history;
    private final RolePermissionDeletionHistoryJpaRepository permissions;
    private final UserRoleDeletionHistoryJpaRepository bindings;
    public java.util.Optional<RoleDeletionHistorySnapshot> findByOriginalId(Long id) {
        return history.findById(id).map(e -> new RoleDeletionHistorySnapshot(e.getOriginalId(), e.getName(), e.getDescription(), e.getKind(), e.getPreviousStatus(), e.getCreatedAt(), e.getUpdatedAt(), e.getArchivedAt(), e.getDeletedByUserId(), e.getDeletedAt(), e.getReason(), e.isInferredTime(), e.getSchemaVersion(), permissions.findByRoleIdOrderByOriginalId(id).stream().map(p -> new RoleDeletionHistoryPermission(p.getOriginalId(),p.getPermissionId(),p.getPermissionCode(),p.getCreatedAt())).toList(),
                bindings.findByRoleIdOrderByOriginalId(id).stream().map(b -> new RoleDeletionHistoryBinding(b.getOriginalId(),b.getUserId(),b.getCreatedAt())).toList()));
    }
}
