package com.jeepclub.backend.billing.core.application.service.history;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionDeletionHistoryQueryRepository;
import com.jeepclub.backend.platform.logging.*;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class ChargeDefinitionDeletionHistoryService {
    private final ChargeDefinitionDeletionHistoryQueryRepository history;
    private final SystemLogService audit;
    private final java.time.Clock clock;
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ChargeDefinitionDeletionHistoryQueryRepository.ChargeDefinitionDeletionHistorySnapshot find(Long id, Long actorId) {
        var result=history.findByOriginalId(id).orElseThrow(() -> new com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException("Deletion history not found."));
        audit.recordRequired(new SystemLogEvent(actorId,"BILLING_CHARGE_DEFINITION_HISTORY_READ","GET","/billing/charge-definitions/"+id+"/deletion-history",
                200,SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        return result;
    }
}
