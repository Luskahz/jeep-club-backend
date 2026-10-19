package com.jeepclub.backend.iam.authorization.core.application.service.history;
import com.jeepclub.backend.iam.authorization.core.repository.RoleDeletionHistoryQueryRepository;
import com.jeepclub.backend.platform.logging.*;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class RoleDeletionHistoryService {
    private final RoleDeletionHistoryQueryRepository history;
    private final SystemLogService audit;
    private final java.time.Clock clock;
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public RoleDeletionHistoryQueryRepository.RoleDeletionHistorySnapshot find(Long id, Long actorId) {
        var result=history.findByOriginalId(id).orElseThrow(() -> new com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException(id));
        audit.recordRequired(new SystemLogEvent(actorId,"AUTHORIZATION_ROLE_HISTORY_READ","GET","/authorization/roles/"+id+"/deletion-history",
                200,SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        return result;
    }
}
