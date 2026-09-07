package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;

public interface MedicalProfileAuditTrail {

    void record(MedicalProfileAuditEvent event);
}
