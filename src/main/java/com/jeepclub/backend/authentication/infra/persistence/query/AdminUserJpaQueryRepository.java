package com.jeepclub.backend.authentication.infra.persistence.query;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.identity.api.module.IdentityStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AdminUserJpaQueryRepository {
    private final EntityManager entityManager;

    public Page<AdminUserResult> findAll(AdminUserFilter filter, Set<AdminUserField> fields, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<AuthenticationAccountEntity> account = query.from(AuthenticationAccountEntity.class);
        Join<AuthenticationAccountEntity, IdentityEntity> identity = account.join("identity");
        query.multiselect(selections(identity, account, cb, fields));
        query.where(predicates(filter, identity, account, cb).toArray(Predicate[]::new));
        applySort(query, identity, account, cb, pageable);
        List<AdminUserResult> content = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(tuple -> toResult(tuple, fields)).toList();
        return new PageImpl<>(content, pageable, count(filter));
    }

    public Optional<AdminUserResult> findById(Long userId) {
        Set<AdminUserField> fields = Set.of(AdminUserField.values());
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<AuthenticationAccountEntity> account = query.from(AuthenticationAccountEntity.class);
        Join<AuthenticationAccountEntity, IdentityEntity> identity = account.join("identity");
        query.multiselect(selections(identity, account, cb, fields));
        query.where(cb.equal(identity.get("id"), userId));
        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(tuple -> toResult(tuple, fields));
    }

    private List<Selection<?>> selections(Join<AuthenticationAccountEntity, IdentityEntity> identity,
                                           Root<AuthenticationAccountEntity> account,
                                           CriteriaBuilder cb, Set<AdminUserField> fields) {
        List<Selection<?>> result = new ArrayList<>();
        for (AdminUserField field : fields) {
            switch (field) {
                case ID -> result.add(identity.get("id").alias("id"));
                case NAME -> result.add(identity.get("name").alias("name"));
                case CPF -> result.add(identity.get("cpf").alias("cpf"));
                case EMAIL -> result.add(identity.get("email").alias("email"));
                case PHONE_NUMBER -> result.add(identity.get("phoneNumber").alias("phoneNumber"));
                case ACCOUNT_STATUS -> result.add(identity.get("status").alias("identityStatus"));
                case AUTHENTICATION_STATUS -> result.add(account.get("authenticationStatus").alias("authenticationStatus"));
                case CREDENTIAL_STATUS -> result.add(account.get("credentialStatus").alias("credentialStatus"));
                case PASSWORD_CHANGE_REQUIRED -> result.add(passwordChangeRequired(account, cb).alias("passwordChangeRequired"));
                case CREATED_AT -> result.add(identity.get("createdAt").alias("createdAt"));
                case UPDATED_AT -> {
                    result.add(identity.get("updatedAt").alias("identityUpdatedAt"));
                    result.add(account.get("updatedAt").alias("accountUpdatedAt"));
                }
            }
        }
        return result;
    }

    private List<Predicate> predicates(AdminUserFilter filter, Join<AuthenticationAccountEntity, IdentityEntity> identity,
                                       Root<AuthenticationAccountEntity> account, CriteriaBuilder cb) {
        List<Predicate> result = new ArrayList<>();
        if (filter.id() != null) result.add(cb.equal(identity.get("id"), filter.id()));
        if (filter.name() != null) result.add(cb.like(cb.lower(identity.get("name")), like(filter.name())));
        if (filter.cpf() != null) result.add(cb.equal(identity.get("cpf"), filter.cpf()));
        if (filter.email() != null) result.add(cb.like(cb.lower(identity.get("email")), like(filter.email())));
        if (filter.phoneNumber() != null) result.add(cb.like(identity.get("phoneNumber"), "%" + filter.phoneNumber() + "%"));
        if (filter.accountStatus() != null) result.add(cb.equal(identity.get("status"),
                filter.accountStatus() == AccountStatus.ACTIVE ? IdentityStatus.ACTIVE : IdentityStatus.DISABLED));
        if (filter.authenticationStatus() != null) result.add(cb.equal(account.get("authenticationStatus"), filter.authenticationStatus()));
        if (filter.credentialStatus() != null) result.add(cb.equal(account.get("credentialStatus"), filter.credentialStatus()));
        if (filter.passwordChangeRequired() != null) {
            Predicate required = account.get("credentialStatus").in(CredentialStatus.CHANGE_REQUIRED, CredentialStatus.PENDING_FIRST_ACCESS);
            result.add(filter.passwordChangeRequired() ? required : cb.not(required));
        }
        if (filter.createdFrom() != null) result.add(cb.greaterThanOrEqualTo(identity.get("createdAt"), filter.createdFrom()));
        if (filter.createdTo() != null) result.add(cb.lessThanOrEqualTo(identity.get("createdAt"), filter.createdTo()));
        Expression<Instant> updatedAt = updatedAt(identity, account, cb);
        if (filter.updatedFrom() != null) result.add(cb.greaterThanOrEqualTo(updatedAt, filter.updatedFrom()));
        if (filter.updatedTo() != null) result.add(cb.lessThanOrEqualTo(updatedAt, filter.updatedTo()));
        if (filter.query() != null) {
            String search = like(filter.query());
            result.add(cb.or(cb.like(cb.lower(identity.get("name")), search),
                    cb.like(cb.lower(identity.get("email")), search), cb.like(identity.get("cpf"), search),
                    cb.like(identity.get("phoneNumber"), search)));
        }
        return result;
    }

    private void applySort(CriteriaQuery<Tuple> query, Join<AuthenticationAccountEntity, IdentityEntity> identity,
                           Root<AuthenticationAccountEntity> account, CriteriaBuilder cb, Pageable pageable) {
        List<Order> orders = pageable.getSort().stream().map(sort -> {
            Expression<?> expression = switch (sort.getProperty()) {
                case "id", "name", "cpf", "email", "phoneNumber", "createdAt" -> identity.get(sort.getProperty());
                case "updatedAt" -> updatedAt(identity, account, cb);
                default -> throw new IllegalArgumentException("Unsupported user sort field: " + sort.getProperty());
            };
            return sort.isAscending() ? cb.asc(expression) : cb.desc(expression);
        }).toList();
        if (!orders.isEmpty()) query.orderBy(orders);
    }

    private long count(AdminUserFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<AuthenticationAccountEntity> account = query.from(AuthenticationAccountEntity.class);
        Join<AuthenticationAccountEntity, IdentityEntity> identity = account.join("identity");
        query.select(cb.count(account));
        query.where(predicates(filter, identity, account, cb).toArray(Predicate[]::new));
        return entityManager.createQuery(query).getSingleResult();
    }

    private Expression<Boolean> passwordChangeRequired(Root<AuthenticationAccountEntity> account, CriteriaBuilder cb) {
        return cb.<Boolean>selectCase().when(account.get("credentialStatus").in(
                CredentialStatus.CHANGE_REQUIRED, CredentialStatus.PENDING_FIRST_ACCESS), true).otherwise(false);
    }

    private Expression<Instant> updatedAt(Join<AuthenticationAccountEntity, IdentityEntity> identity,
                                          Root<AuthenticationAccountEntity> account, CriteriaBuilder cb) {
        Expression<Instant> identityUpdated = identity.get("updatedAt");
        Expression<Instant> accountUpdated = account.get("updatedAt");
        return cb.<Instant>selectCase()
                .when(cb.isNull(identityUpdated), accountUpdated)
                .when(cb.isNull(accountUpdated), identityUpdated)
                .when(cb.greaterThan(identityUpdated, accountUpdated), identityUpdated)
                .otherwise(accountUpdated);
    }

    private AdminUserResult toResult(Tuple tuple, Set<AdminUserField> fields) {
        IdentityStatus status = get(tuple, fields, AdminUserField.ACCOUNT_STATUS, "identityStatus", IdentityStatus.class);
        Instant identityUpdated = get(tuple, fields, AdminUserField.UPDATED_AT, "identityUpdatedAt", Instant.class);
        Instant accountUpdated = get(tuple, fields, AdminUserField.UPDATED_AT, "accountUpdatedAt", Instant.class);
        return new AdminUserResult(get(tuple, fields, AdminUserField.ID, "id", Long.class),
                get(tuple, fields, AdminUserField.NAME, "name", String.class),
                get(tuple, fields, AdminUserField.CPF, "cpf", String.class),
                get(tuple, fields, AdminUserField.EMAIL, "email", String.class),
                get(tuple, fields, AdminUserField.PHONE_NUMBER, "phoneNumber", String.class),
                status == null ? null : (status == IdentityStatus.ACTIVE ? AccountStatus.ACTIVE : AccountStatus.DISABLED),
                get(tuple, fields, AdminUserField.AUTHENTICATION_STATUS, "authenticationStatus", AuthenticationStatus.class),
                get(tuple, fields, AdminUserField.CREDENTIAL_STATUS, "credentialStatus", CredentialStatus.class),
                get(tuple, fields, AdminUserField.PASSWORD_CHANGE_REQUIRED, "passwordChangeRequired", Boolean.class),
                get(tuple, fields, AdminUserField.CREATED_AT, "createdAt", Instant.class), latest(identityUpdated, accountUpdated));
    }

    private <T> T get(Tuple tuple, Set<AdminUserField> fields, AdminUserField field, String alias, Class<T> type) {
        return fields.contains(field) ? tuple.get(alias, type) : null;
    }

    private static Instant latest(Instant first, Instant second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }

    private static String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
