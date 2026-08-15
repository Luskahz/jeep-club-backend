package com.jeepclub.backend.authentication.infra.persistence.query;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity_;
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
                                root.get(UserEntity_.id)
                                        .alias("id")
                        );

                case NAME ->
                        selections.add(
                                root.get(UserEntity_.name)
                                        .alias("name")
                        );

                case CPF ->
                        selections.add(
                                root.get(UserEntity_.cpf)
                                        .alias("cpf")
                        );

                case EMAIL ->
                        selections.add(
                                root.get(UserEntity_.email)
                                        .alias("email")
                        );

                case PHONE_NUMBER ->
                        selections.add(
                                root.get(UserEntity_.phoneNumber)
                                        .alias("phoneNumber")
                        );

                case ACCOUNT_STATUS ->
                        selections.add(
                                root.get(UserEntity_.accountStatus)
                                        .alias("accountStatus")
                        );

                case AUTHENTICATION_STATUS ->
                        selections.add(
                                root.get(UserEntity_.authenticationStatus)
                                        .alias("authenticationStatus")
                        );

                case CREDENTIAL_STATUS ->
                        selections.add(
                                root.get(UserEntity_.credentialStatus)
                                        .alias("credentialStatus")
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
                                root.get(UserEntity_.createdAt)
                                        .alias("createdAt")
                        );

                case UPDATED_AT ->
                        selections.add(
                                root.get(UserEntity_.updatedAt)
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
                        AdminUserField.ACCOUNT_STATUS,
                        "accountStatus",
                        AccountStatus.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.AUTHENTICATION_STATUS,
                        "authenticationStatus",
                        AuthenticationStatus.class
                ),
                get(
                        tuple,
                        fields,
                        AdminUserField.CREDENTIAL_STATUS,
                        "credentialStatus",
                        CredentialStatus.class
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

    private Expression<Boolean> passwordChangeRequiredExpression(
            Root<UserEntity> root,
            CriteriaBuilder criteriaBuilder
    ) {
        CriteriaBuilder.Case<Boolean> expression =
                criteriaBuilder.selectCase();

        return expression
                .when(
                        root.get(UserEntity_.credentialStatus).in(
                                CredentialStatus.CHANGE_REQUIRED,
                                CredentialStatus.PENDING_FIRST_ACCESS
                        ),
                        Boolean.TRUE
                )
                .otherwise(Boolean.FALSE);
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
                        .map(order -> {
                            Expression<?> expression =
                                    sortExpression(
                                            root,
                                            order.getProperty()
                                    );

                            return order.isAscending()
                                    ? criteriaBuilder.asc(expression)
                                    : criteriaBuilder.desc(expression);
                        })
                        .toList();

        if (!orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
    }

    private Expression<?> sortExpression(
            Root<UserEntity> root,
            String property
    ) {
        return switch (property) {

            case "id" ->
                    root.get(UserEntity_.id);

            case "name" ->
                    root.get(UserEntity_.name);

            case "cpf" ->
                    root.get(UserEntity_.cpf);

            case "email" ->
                    root.get(UserEntity_.email);

            case "phoneNumber" ->
                    root.get(UserEntity_.phoneNumber);

            case "createdAt" ->
                    root.get(UserEntity_.createdAt);

            case "updatedAt" ->
                    root.get(UserEntity_.updatedAt);

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported user sort field: " + property
                    );
        };
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