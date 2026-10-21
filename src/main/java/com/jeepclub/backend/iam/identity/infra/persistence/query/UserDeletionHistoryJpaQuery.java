package com.jeepclub.backend.iam.identity.infra.persistence.query;
import com.jeepclub.backend.iam.identity.core.repository.UserDeletionHistoryQueryRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.*;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class UserDeletionHistoryJpaQuery implements UserDeletionHistoryQueryRepository {
    private final UserDeletionHistoryJpaRepository history;
    public java.util.Optional<UserDeletionHistorySnapshot> findByOriginalId(Long id) {
        return history.findById(id).map(e -> new UserDeletionHistorySnapshot(e.getOriginalId(), e.getPreviousStatus(), e.getCreatedAt(), e.getDisabledAt(), e.getArchivedAt(), e.getUpdatedAt(), e.getDeletedByUserId(), e.getDeletedAt(), e.getReason(), e.getSchemaVersion()));
    }
}
