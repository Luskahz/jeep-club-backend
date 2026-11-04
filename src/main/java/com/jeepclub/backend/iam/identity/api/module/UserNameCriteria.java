package com.jeepclub.backend.iam.identity.api.module;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Read-only name predicate for persistence queries in the shared persistence unit.
 * Identity owns the mapping and literal name semantics; no entity, SQL fragment or
 * matching-ID collection crosses this boundary. Building a predicate executes no query.
 * Consumers must bound their projected result and apply the same predicate to count.
 */
public interface UserNameCriteria {
    /** Literal, trimmed, case-insensitive name contains; all administrative statuses. */
    Predicate matches(CriteriaBuilder builder, CommonAbstractCriteria query, Expression<Long> userId, String name);
}
