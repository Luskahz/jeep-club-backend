package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ToolSpecifications {

    private ToolSpecifications() {}

    public static Specification<ToolEntity> withFilters(String name, ToolStatus status) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (name != null && !name.isBlank()) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
                );
            }

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            return predicate;
        };
    }
}