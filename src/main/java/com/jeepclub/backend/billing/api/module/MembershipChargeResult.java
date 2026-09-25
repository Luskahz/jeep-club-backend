package com.jeepclub.backend.billing.api.module;

public enum MembershipChargeResult {
    WITHIN_PAYMENT_PERIOD,
    SATISFIED,
    CANCELED,
    PAYMENT_REQUIRED,
    CHARGE_NOT_FOUND
}
