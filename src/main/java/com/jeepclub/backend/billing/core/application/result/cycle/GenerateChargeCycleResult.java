package com.jeepclub.backend.billing.core.application.result.cycle;

public record GenerateChargeCycleResult(
        ChargeCycleResult chargeCycle,
        int createdMemberCharges
) {
}