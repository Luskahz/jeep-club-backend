package com.jeepclub.backend.billing.api.module;

public interface MembershipChargeQuery {

    MembershipChargeResult evaluate(Long chargeDefinitionId, Long userId);
}
