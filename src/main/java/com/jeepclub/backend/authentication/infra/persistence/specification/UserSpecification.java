package com.jeepclub.backend.authentication.infra.persistence.specification;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> from(AdminUserFilter filter) {
        Objects.requireNonNull(filter, "filter cannot be null");

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
                        statusPredicate(
                                root,
                                criteriaBuilder,
                                filter.status()
                        )
                );
            }

            if (filter.passwordChangeRequired() != null) {
                predicates.add(
                        passwordChangeRequiredPredicate(
                                root,
                                criteriaBuilder,
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

                predicates.add(
                        criteriaBuilder.or(
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
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static Predicate statusPredicate(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder,
            UserStatus status
    ) {
        return switch (status) {

            case DISABLED ->
                    criteriaBuilder.equal(
                            root.get("accountStatus"),
                            AccountStatus.DISABLED
                    );

            case LOCKED ->
                    criteriaBuilder.and(
                            criteriaBuilder.notEqual(
                                    root.get("accountStatus"),
                                    AccountStatus.DISABLED
                            ),
                            criteriaBuilder.equal(
                                    root.get("authenticationStatus"),
                                    AuthenticationStatus.LOCKED
                            )
                    );

            case CHANGE_PASSWORD_REQUIRED ->
                    criteriaBuilder.and(
                            criteriaBuilder.notEqual(
                                    root.get("accountStatus"),
                                    AccountStatus.DISABLED
                            ),
                            criteriaBuilder.notEqual(
                                    root.get("authenticationStatus"),
                                    AuthenticationStatus.LOCKED
                            ),
                            criteriaBuilder.equal(
                                    root.get("credentialStatus"),
                                    CredentialStatus.CHANGE_REQUIRED
                            )
                    );

            case PENDING_FIRST_ACCESS ->
                    criteriaBuilder.and(
                            criteriaBuilder.notEqual(
                                    root.get("accountStatus"),
                                    AccountStatus.DISABLED
                            ),
                            criteriaBuilder.notEqual(
                                    root.get("authenticationStatus"),
                                    AuthenticationStatus.LOCKED
                            ),
                            criteriaBuilder.equal(
                                    root.get("credentialStatus"),
                                    CredentialStatus.PENDING_FIRST_ACCESS
                            )
                    );

            case ACTIVE ->
                    criteriaBuilder.and(
                            criteriaBuilder.notEqual(
                                    root.get("accountStatus"),
                                    AccountStatus.DISABLED
                            ),
                            criteriaBuilder.notEqual(
                                    root.get("authenticationStatus"),
                                    AuthenticationStatus.LOCKED
                            ),
                            criteriaBuilder.not(
                                    root.get("credentialStatus").in(
                                            CredentialStatus.CHANGE_REQUIRED,
                                            CredentialStatus.PENDING_FIRST_ACCESS
                                    )
                            )
                    );
        };
    }

    private static Predicate passwordChangeRequiredPredicate(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder,
            boolean required
    ) {
        Predicate requiresPasswordChange =
                root.get("credentialStatus").in(
                        CredentialStatus.CHANGE_REQUIRED,
                        CredentialStatus.PENDING_FIRST_ACCESS
                );

        return required
                ? requiresPasswordChange
                : criteriaBuilder.not(requiresPasswordChange);
    }

    private static String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}