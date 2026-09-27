package com.jeepclub.backend.billing.api.module;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventChargeCatalogQuery {
    Page<Entry> findEligible(Pageable pageable);
    Entry getEligible(Long definitionId);
    record Entry(Long id, String name, String description, BigDecimal defaultAmount,
                 boolean required, String paymentAcceptancePolicy) {}
}
