package com.jeepclub.backend.billing.core.application.result;

public record GenerateChargeCycleResult(
        ChargeCycleResult chargeCycle,
        int createdMemberCharges
) {
}