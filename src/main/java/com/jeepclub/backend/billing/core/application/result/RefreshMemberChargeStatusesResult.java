package com.jeepclub.backend.billing.core.application.result;

public record RefreshMemberChargeStatusesResult(
        int processedCharges,
        int markedOverdueCharges,
        int expiredCharges,
        int unchangedCharges
) {

    public RefreshMemberChargeStatusesResult {
        if (processedCharges < 0) {
            throw new IllegalArgumentException("processedCharges cannot be negative.");
        }

        if (markedOverdueCharges < 0) {
            throw new IllegalArgumentException("markedOverdueCharges cannot be negative.");
        }

        if (expiredCharges < 0) {
            throw new IllegalArgumentException("expiredCharges cannot be negative.");
        }

        if (unchangedCharges < 0) {
            throw new IllegalArgumentException("unchangedCharges cannot be negative.");
        }
    }
}