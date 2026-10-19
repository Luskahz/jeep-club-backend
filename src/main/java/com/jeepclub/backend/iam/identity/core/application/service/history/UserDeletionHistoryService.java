package com.jeepclub.backend.iam.identity.core.application.service.history;
import com.jeepclub.backend.iam.identity.core.repository.UserDeletionHistoryQueryRepository;
import com.jeepclub.backend.platform.logging.*;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class UserDeletionHistoryService {
    private final UserDeletionHistoryQueryRepository history;
    private final SystemLogService audit;
    private final java.time.Clock clock;
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public UserDeletionHistoryQueryRepository.UserDeletionHistorySnapshot find(Long id, Long actorId) {
        var result=history.findByOriginalId(id).orElseThrow(() -> new com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException(id));
        audit.recordRequired(new SystemLogEvent(actorId,"IDENTITY_USER_HISTORY_READ","GET","/identity/admin/users/"+id+"/deletion-history",
                200,SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        return result;
    }
}
