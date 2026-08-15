package com.jeepclub.backend.authentication.infra.persistence.specification;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> from(AdminUserFilter filter) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.id() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("id"),
                                filter.id()
                        )
                );
            }

            if (filter.name() != null) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                like(filter.name())
                        )
                );
            }

            if (filter.cpf() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("cpf"),
                                filter.cpf()
                        )
                );
            }

            if (filter.email() != null) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                like(filter.email())
                        )
                );
            }

            if (filter.phone() != null) {
                predicates.add(
                        criteriaBuilder.like(
                                root.get("phoneNumber"),
                                "%" + filter.phone() + "%"
                        )
                );
            }

            if (filter.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                filter.status()
                        )
                );
            }

            if (filter.passwordChangeRequired() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("changePasswordRequired"),
                                filter.passwordChangeRequired()
                        )
                );
            }

            if (filter.createdFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                filter.createdFrom()
                        )
                );
            }

            if (filter.createdTo() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdAt"),
                                filter.createdTo()
                        )
                );
            }

            if (filter.updatedFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("updatedAt"),
                                filter.updatedFrom()
                        )
                );
            }

            if (filter.updatedTo() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("updatedAt"),
                                filter.updatedTo()
                        )
                );
            }

            if (filter.query() != null) {
                String search = like(filter.query());

                Predicate queryPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                search
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                search
                        ),
                        criteriaBuilder.like(
                                root.get("cpf"),
                                search
                        ),
                        criteriaBuilder.like(
                                root.get("phoneNumber"),
                                search
                        )
                );

                predicates.add(queryPredicate);
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}