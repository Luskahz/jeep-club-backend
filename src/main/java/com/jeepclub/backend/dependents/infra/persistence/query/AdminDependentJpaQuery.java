package com.jeepclub.backend.dependents.infra.persistence.query;

import com.jeepclub.backend.dependents.core.application.exception.InvalidDependentSearchException;
import com.jeepclub.backend.dependents.core.application.query.AdminDependentField;
import com.jeepclub.backend.dependents.core.application.query.AdminDependentFilter;
import com.jeepclub.backend.dependents.core.application.result.AdminDependentSearchResult;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.iam.identity.api.module.UserNameCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Module-local tuple query; no Identity persistence access and no entity materialization. */
public final class AdminDependentJpaQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "cpf",
            "birthDate",
            "relationshipType",
            "phoneNumber",
            "userId",
            "status",
            "createdAt",
            "updatedAt",
            "archivedAt"
    );

    private final EntityManager em;
    private final UserNameCriteria ownerNames;

    public AdminDependentJpaQuery(EntityManager em, UserNameCriteria ownerNames) {
        this.em = em;
        this.ownerNames = ownerNames;
    }

    public Page<AdminDependentSearchResult> findAll(
            AdminDependentFilter filter,
            Set<AdminDependentField> fields,
            Pageable pageable
    ) {
        Sort sort = pageable.getSort();
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidDependentSearchException("Invalid dependent sort field: " + order.getProperty());
            }
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }

        Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        Root<DependentEntity> root = query.from(DependentEntity.class);

        Set<AdminDependentField> persisted = EnumSet.copyOf(fields);
        boolean needPhoto = persisted.remove(AdminDependentField.PHOTO_URL);
        if (persisted.remove(AdminDependentField.OWNER_NAME)) {
            persisted.add(AdminDependentField.USER_ID);
        }
        if (needPhoto) {
            persisted.add(AdminDependentField.ID);
            persisted.add(AdminDependentField.USER_ID);
        }

        List<Selection<?>> columns = new ArrayList<>();
        for (AdminDependentField field : persisted) {
            columns.add(root.get(field.property()).alias(field.property()));
        }
        if (needPhoto) {
            columns.add(root.get("photoStorageKey").alias("photoStorageKey"));
        }

        query.multiselect(columns).where(predicates(filter, root, cb, query).toArray(Predicate[]::new));
        query.orderBy(sort.stream()
                .map(o -> o.isAscending() ? cb.asc(root.get(o.getProperty())) : cb.desc(root.get(o.getProperty())))
                .toList());

        List<Tuple> rows = em.createQuery(query)
                .setFirstResult(Math.toIntExact(stable.getOffset()))
                .setMaxResults(stable.getPageSize())
                .getResultList();

        var countQuery = cb.createQuery(Long.class);
        Root<DependentEntity> counted = countQuery.from(DependentEntity.class);
        countQuery.select(cb.count(counted)).where(predicates(filter, counted, cb, countQuery).toArray(Predicate[]::new));

        long total = em.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(
                rows.stream().map(t -> result(t, fields, persisted, needPhoto)).toList(),
                stable,
                total
        );
    }

    private List<Predicate> predicates(
            AdminDependentFilter filter,
            Root<DependentEntity> root,
            CriteriaBuilder cb,
            CommonAbstractCriteria query
    ) {
        List<Predicate> result = new ArrayList<>();
        if (filter.status() != null) {
            result.add(cb.equal(root.get("status"), filter.status()));
        }
        if (filter.userId() != null) {
            result.add(cb.equal(root.get("userId"), filter.userId()));
        }
        if (filter.relationshipType() != null) {
            result.add(cb.equal(root.get("relationshipType"), filter.relationshipType()));
        }
        if (filter.name() != null) {
            result.add(cb.like(cb.lower(root.get("name")), literal(filter.name()), '!'));
        }
        if (filter.cpf() != null) {
            result.add(cb.like(root.get("cpf"), literal(filter.cpf()), '!'));
        }
        if (filter.q() != null) {
            String pattern = literal(filter.q());
            String numericQuery = AdminDependentFilter.extractNumericQueryIfValid(filter.q());
            List<Predicate> orBranches = new ArrayList<>();
            orBranches.add(cb.like(cb.lower(root.get("name")), pattern, '!'));
            if (numericQuery != null) {
                String numericPattern = literal(numericQuery);
                orBranches.add(cb.like(root.get("cpf"), numericPattern, '!'));
                orBranches.add(cb.like(root.get("phoneNumber"), numericPattern, '!'));
            }
            if (ownerNames != null) {
                orBranches.add(ownerNames.matches(cb, query, root.get("userId"), filter.q()));
            }
            result.add(cb.or(orBranches.toArray(Predicate[]::new)));
        }
        return result;
    }

    private static String literal(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
    }

    private static <T> T get(
            Tuple tuple,
            Set<AdminDependentField> persisted,
            AdminDependentField field,
            Class<T> type
    ) {
        return persisted.contains(field) ? tuple.get(field.property(), type) : null;
    }

    private AdminDependentSearchResult result(
            Tuple t,
            Set<AdminDependentField> requested,
            Set<AdminDependentField> persisted,
            boolean needPhoto
    ) {
        Long tupleId = get(t, persisted, AdminDependentField.ID, Long.class);
        Long tupleUserId = get(t, persisted, AdminDependentField.USER_ID, Long.class);
        String photoStorageKey = needPhoto ? t.get("photoStorageKey", String.class) : null;
        String photoUrl = needPhoto && photoStorageKey != null && tupleUserId != null && tupleId != null
                ? "/users/" + tupleUserId + "/dependents/" + tupleId + "/photo"
                : null;

        Long exposedId = requested.contains(AdminDependentField.ID) ? tupleId : null;
        Long exposedUserId = requested.contains(AdminDependentField.USER_ID) || requested.contains(AdminDependentField.OWNER_NAME)
                ? tupleUserId
                : null;

        return new AdminDependentSearchResult(
                exposedId,
                get(t, persisted, AdminDependentField.NAME, String.class),
                get(t, persisted, AdminDependentField.CPF, String.class),
                get(t, persisted, AdminDependentField.BIRTH_DATE, LocalDate.class),
                get(t, persisted, AdminDependentField.RELATIONSHIP_TYPE, RelationshipType.class),
                get(t, persisted, AdminDependentField.PHONE_NUMBER, String.class),
                exposedUserId,
                null,
                get(t, persisted, AdminDependentField.STATUS, DependentStatus.class),
                get(t, persisted, AdminDependentField.CREATED_AT, Instant.class),
                get(t, persisted, AdminDependentField.UPDATED_AT, Instant.class),
                get(t, persisted, AdminDependentField.ARCHIVED_AT, Instant.class),
                photoUrl
        );
    }
}
