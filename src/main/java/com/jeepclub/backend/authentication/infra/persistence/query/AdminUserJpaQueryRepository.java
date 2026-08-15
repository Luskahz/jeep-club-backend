package com.jeepclub.backend.authentication.infra.persistence.query;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.specification.UserSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AdminUserJpaQueryRepository {

    private final EntityManager entityManager;

    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        CriteriaBuilder criteriaBuilder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> criteriaQuery =
                criteriaBuilder.createTupleQuery();

        Root<UserEntity> root =
                criteriaQuery.from(UserEntity.class);

        criteriaQuery.multiselect(
                createSelections(
                        root,
                        criteriaBuilder,
                        fields
                )
        );

        Predicate predicate =
                UserSpecification.from(filter)
                        .toPredicate(
                                root,
                                criteriaQuery,
                                criteriaBuilder
                        );

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }

        applySort(
                criteriaQuery,
                root,
                criteriaBuilder,
                pageable
        );

        TypedQuery<Tuple> query =
                entityManager.createQuery(criteriaQuery);

        query.setFirstResult(
                Math.toIntExact(pageable.getOffset())
        );

        query.setMaxResults(
                pageable.getPageSize()
        );

        List<AdminUserResult> content =
                query.getResultList()
                        .stream()
                        .map(tuple -> toResult(tuple, fields))
                        .toList();

        long total = count(filter);

        return new PageImpl<>(
                content,
                pageable,
                total
        );
    }

    private List<Selection<?>> createSelections(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder,
            Set<AdminUserField> fields
    ) {
        List<Selection<?>> selections =
                new ArrayList<>();

        for (AdminUserField field : fields) {

            switch (field) {

                case ID ->
                        selections.add(
                                root.get("id")
                                        .alias("id")
                        );

                case NAME ->
                        selections.add(
                                root.get("name")
                                        .alias("name")
                        );

                case CPF ->
                        selections.add(
                                root.get("cpf")
                                        .alias("cpf")
                        );

                case EMAIL ->
                        selections.add(
                                root.get("email")
                                        .alias("email")
                        );

                case PHONE_NUMBER ->
                        selections.add(
                                root.get("phoneNumber")
                                        .alias("phoneNumber")
                        );

                case STATUS ->
                        selections.add(
                                statusExpression(
                                        root,
                                        criteriaBuilder
                                ).alias("status")
                        );

                case PASSWORD_CHANGE_REQUIRED ->
                        selections.add(
                                passwordChangeRequiredExpression(
                                        root,
                                        criteriaBuilder
                                ).alias("passwordChangeRequired")
                        );

                case CREATED_AT ->
                        selections.add(
                                root.get("createdAt")
                                        .alias("createdAt")
                        );

                case UPDATED_AT ->
                        selections.add(
                                root.get("updatedAt")
                                        .alias("updatedAt")
                        );
            }
        }

        return selections;
    }

    private AdminUserResult toResult(
            Tuple tuple,
            Set<AdminUserField> fields
    ) {
        return new AdminUserResult(
                get(
                        tuple,
                        fields,
                        AdminUserField.ID,
                        "id",
                        Long.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.NAME,
                        "name",
                        String.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.CPF,
                        "cpf",
                        String.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.EMAIL,
                        "email",
                        String.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.PHONE_NUMBER,
                        "phoneNumber",
                        String.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.STATUS,
                        "status",
                        String.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.PASSWORD_CHANGE_REQUIRED,
                        "passwordChangeRequired",
                        Boolean.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.CREATED_AT,
                        "createdAt",
                        Instant.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.UPDATED_AT,
                        "updatedAt",
                        Instant.class
                )
        );
    }

    private <T> T get(
            Tuple tuple,
            Set<AdminUserField> fields,
            AdminUserField field,
            String alias,
            Class<T> type
    ) {
        if (!fields.contains(field)) {
            return null;
        }

        return tuple.get(alias, type);
    }

    private Expression<String> statusExpression(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder
    ) {
        return criteriaBuilder.<String>selectCase()

                .when(
                        criteriaBuilder.equal(
                                root.get("accountStatus"),
                                AccountStatus.DISABLED
                        ),
                        UserStatus.DISABLED.name()
                )

                .when(
                        criteriaBuilder.equal(
                                root.get("authenticationStatus"),
                                AuthenticationStatus.LOCKED
                        ),
                        UserStatus.LOCKED.name()
                )

                .when(
                        criteriaBuilder.equal(
                                root.get("credentialStatus"),
                                CredentialStatus.CHANGE_REQUIRED
                        ),
                        UserStatus.CHANGE_PASSWORD_REQUIRED.name()
                )

                .when(
                        criteriaBuilder.equal(
                                root.get("credentialStatus"),
                                CredentialStatus.PENDING_FIRST_ACCESS
                        ),
                        UserStatus.PENDING_FIRST_ACCESS.name()
                )

                .otherwise(
                        UserStatus.ACTIVE.name()
                );
    }

    private Expression<Boolean> passwordChangeRequiredExpression(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder
    ) {
        return criteriaBuilder.<Boolean>selectCase()

                .when(
                        root.get("credentialStatus").in(
                                CredentialStatus.CHANGE_REQUIRED,
                                CredentialStatus.PENDING_FIRST_ACCESS
                        ),
                        true
                )

                .otherwise(false);
    }

    private void applySort(
            CriteriaQuery<Tuple> criteriaQuery,
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder,
            Pageable pageable
    ) {
        List<Order> orders =
                pageable.getSort()
                        .stream()
                        .map(order ->
                                order.isAscending()
                                        ? criteriaBuilder.asc(
                                        root.get(order.getProperty())
                                )
                                        : criteriaBuilder.desc(
                                        root.get(order.getProperty())
                                )
                        )
                        .toList();

        if (!orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
    }

    private long count(
            AdminUserFilter filter
    ) {
        CriteriaBuilder criteriaBuilder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> criteriaQuery =
                criteriaBuilder.createQuery(Long.class);

        Root<UserEntity> root =
                criteriaQuery.from(UserEntity.class);

        criteriaQuery.select(
                criteriaBuilder.count(root)
        );

        Predicate predicate =
                UserSpecification.from(filter)
                        .toPredicate(
                                root,
                                criteriaQuery,
                                criteriaBuilder
                        );

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }

        return entityManager
                .createQuery(criteriaQuery)
                .getSingleResult();
    }
}