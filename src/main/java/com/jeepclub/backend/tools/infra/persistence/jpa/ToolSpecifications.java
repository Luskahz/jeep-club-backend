package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ToolSpecifications {

    private ToolSpecifications() {}

    public static Specification<ToolEntity> withFilters(String name, ToolStatus status) {
        return administrative(new com.jeepclub.backend.tools.core.application.query.AdminToolFilter(null,name,status,null));
    }

    public static Specification<ToolEntity> administrative(com.jeepclub.backend.tools.core.application.query.AdminToolFilter filter) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (filter.name() != null) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("name")), literal(filter.name()), '!')
                );
            }

            if (filter.status() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), filter.status()));
            }
            if (filter.userId() != null) predicate = cb.and(predicate, cb.equal(root.get("userId"), filter.userId()));
            if (filter.q() != null) predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), literal(filter.q()), '!'));

            return predicate;
        };
    }

    private static String literal(String value) {
        return "%" + value.toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_") + "%";
    }
}
