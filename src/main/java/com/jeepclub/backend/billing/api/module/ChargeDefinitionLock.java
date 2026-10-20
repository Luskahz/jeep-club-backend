package com.jeepclub.backend.billing.api.module;
/** Transactional eligibility read for writers of logical definition references. */
public interface ChargeDefinitionLock { boolean isActive(Long definitionId); }
