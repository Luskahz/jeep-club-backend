package com.jeepclub.backend.identity.infra.persistence.query;

import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<UserEntity> user = query.from(UserEntity.class);
        query.multiselect(selections(user, fields));
        query.where(predicates(filter, user, cb).toArray(Predicate[]::new));
        applySort(query, user, cb, pageable);

        List<AdminUserResult> content = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList().stream()
                .map(tuple -> toResult(tuple, fields))
                .toList();
        return new PageImpl<>(content, pageable, count(filter));
    }

    public Optional<AdminUserResult> findById(Long userId) {
        Set<AdminUserField> fields = Set.of(AdminUserField.values());
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<UserEntity> user = query.from(UserEntity.class);
        query.multiselect(selections(user, fields));
        query.where(cb.equal(user.get("id"), userId));
        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(tuple -> toResult(tuple, fields));
    }

    private List<Selection<?>> selections(Root<UserEntity> user, Set<AdminUserField> fields) {
        List<Selection<?>> result = new ArrayList<>();
        for (AdminUserField field : fields) {
            String property = property(field);
            result.add(user.get(property).alias(property));
        }
        return result;
    }

    private List<Predicate> predicates(
            AdminUserFilter filter,
            Root<UserEntity> user,
            CriteriaBuilder cb
    ) {
        List<Predicate> result = new ArrayList<>();
        if (filter.id() != null) result.add(cb.equal(user.get("id"), filter.id()));
        if (filter.name() != null) result.add(cb.like(cb.lower(user.get("name")), like(filter.name())));
        if (filter.birthDate() != null) result.add(cb.equal(user.get("birthDate"), filter.birthDate()));
        if (filter.email() != null) result.add(cb.like(cb.lower(user.get("email")), like(filter.email())));
        if (filter.cpf() != null) result.add(cb.equal(user.get("cpf"), filter.cpf()));
        if (filter.rg() != null) result.add(cb.equal(user.get("rg"), filter.rg()));
        if (filter.phoneNumber() != null) {
            result.add(cb.like(user.get("phoneNumber"), "%" + filter.phoneNumber() + "%"));
        }
        if (filter.status() != null) result.add(cb.equal(user.get("status"), filter.status()));
        if (filter.createdFrom() != null) {
            result.add(cb.greaterThanOrEqualTo(user.get("createdAt"), filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            result.add(cb.lessThanOrEqualTo(user.get("createdAt"), filter.createdTo()));
        }
        if (filter.updatedFrom() != null) {
            result.add(cb.greaterThanOrEqualTo(user.get("updatedAt"), filter.updatedFrom()));
        }
        if (filter.updatedTo() != null) {
            result.add(cb.lessThanOrEqualTo(user.get("updatedAt"), filter.updatedTo()));
        }
        if (filter.query() != null) {
            String search = like(filter.query());
            result.add(cb.or(
                    cb.like(cb.lower(user.get("name")), search),
                    cb.like(cb.lower(user.get("email")), search),
                    cb.like(user.get("cpf"), search),
                    cb.like(user.get("rg"), search),
                    cb.like(user.get("phoneNumber"), search)
            ));
        }
        return result;
    }

    private void applySort(
            CriteriaQuery<Tuple> query,
            Root<UserEntity> user,
            CriteriaBuilder cb,
            Pageable pageable
    ) {
        List<Order> orders = pageable.getSort().stream()
                .map(sort -> sort.isAscending()
                        ? cb.asc(user.get(sort.getProperty()))
                        : cb.desc(user.get(sort.getProperty())))
                .toList();
        if (!orders.isEmpty()) query.orderBy(orders);
    }

    private long count(AdminUserFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UserEntity> user = query.from(UserEntity.class);
        query.select(cb.count(user));
        query.where(predicates(filter, user, cb).toArray(Predicate[]::new));
        return entityManager.createQuery(query).getSingleResult();
    }

    private AdminUserResult toResult(Tuple tuple, Set<AdminUserField> fields) {
        return new AdminUserResult(
                get(tuple, fields, AdminUserField.ID, "id", Long.class),
                get(tuple, fields, AdminUserField.NAME, "name", String.class),
                get(tuple, fields, AdminUserField.BIRTH_DATE, "birthDate", LocalDate.class),
                get(tuple, fields, AdminUserField.EMAIL, "email", String.class),
                get(tuple, fields, AdminUserField.CPF, "cpf", String.class),
                get(tuple, fields, AdminUserField.RG, "rg", String.class),
                get(tuple, fields, AdminUserField.PHONE_NUMBER, "phoneNumber", String.class),
                get(tuple, fields, AdminUserField.PROFILE_PHOTO_URL, "profilePhotoUrl", String.class),
                get(tuple, fields, AdminUserField.STATUS, "status", UserStatus.class),
                get(tuple, fields, AdminUserField.CREATED_AT, "createdAt", Instant.class),
                get(tuple, fields, AdminUserField.DISABLED_AT, "disabledAt", Instant.class),
                get(tuple, fields, AdminUserField.UPDATED_AT, "updatedAt", Instant.class)
        );
    }

    private <T> T get(
            Tuple tuple,
            Set<AdminUserField> fields,
            AdminUserField field,
            String alias,
            Class<T> type
    ) {
        return fields.contains(field) ? tuple.get(alias, type) : null;
    }

    private static String property(AdminUserField field) {
        return switch (field) {
            case ID -> "id";
            case NAME -> "name";
            case BIRTH_DATE -> "birthDate";
            case EMAIL -> "email";
            case CPF -> "cpf";
            case RG -> "rg";
            case PHONE_NUMBER -> "phoneNumber";
            case PROFILE_PHOTO_URL -> "profilePhotoUrl";
            case STATUS -> "status";
            case CREATED_AT -> "createdAt";
            case DISABLED_AT -> "disabledAt";
            case UPDATED_AT -> "updatedAt";
        };
    }

    private static String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
