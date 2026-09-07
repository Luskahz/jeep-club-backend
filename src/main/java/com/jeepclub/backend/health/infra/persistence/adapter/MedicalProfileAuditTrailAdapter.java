package com.jeepclub.backend.health.infra.persistence.adapter;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class MedicalProfileAuditTrailAdapter implements MedicalProfileAuditTrail {

    private final MedicalProfileAuditWriter writer;

    @Override
    public void record(MedicalProfileAuditEvent event) {
        if (event.outcome() == MedicalProfileAuditOutcome.SUCCEEDED
                && TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            writer.write(event);
                        }
                    }
            );
            return;
        }

        writer.write(event);
    }
}
