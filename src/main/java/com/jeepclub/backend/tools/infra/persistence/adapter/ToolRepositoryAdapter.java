package com.jeepclub.backend.tools.infra.persistence.adapter;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAlreadyDeletedException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolHistoryEntity;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolHistoryJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolSpecifications;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolHistoryMapper;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ToolRepositoryAdapter implements ToolRepository {
    private final jakarta.persistence.EntityManager entityManager;


    private final ToolJpaRepository jpaRepository;
    private final ToolHistoryJpaRepository historyJpaRepository;
    private final ToolMapper mapper;
    private final ToolHistoryMapper historyMapper;

    @Override
    public Page<com.jeepclub.backend.tools.core.application.result.AdminToolResult> search(
            com.jeepclub.backend.tools.core.application.query.AdminToolFilter filter,
            java.util.Set<com.jeepclub.backend.tools.core.application.query.AdminToolField> fields, Pageable pageable) {
        var sort = pageable.getSort();
        var allowed = java.util.Set.of("id","name","status","userId","description","photoStorageKey","createdAt","updatedAt","archivedAt");
        for (var order : sort) if (!allowed.contains(order.getProperty()))
            throw new com.jeepclub.backend.tools.core.application.exception.InvalidToolSortException(order.getProperty());
        if (sort.getOrderFor("id") == null) sort = sort.and(org.springframework.data.domain.Sort.by("id"));
        var stable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(),sort);
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createTupleQuery(); var root = query.from(ToolEntity.class);
        java.util.List<jakarta.persistence.criteria.Selection<?>> selections = new java.util.ArrayList<>();
        for (var field : fields) selections.add(root.get(field.property()).alias(field.property()));
        query.multiselect(selections);
        var specification = ToolSpecifications.administrative(filter);
        query.where(specification.toPredicate(root,query,cb));
        query.orderBy(sort.stream().map(o -> o.isAscending()?cb.asc(root.get(o.getProperty())):cb.desc(root.get(o.getProperty()))).toList());
        var content = entityManager.createQuery(query).setFirstResult(Math.toIntExact(stable.getOffset()))
                .setMaxResults(stable.getPageSize()).getResultList().stream().map(t ->
                    new com.jeepclub.backend.tools.core.application.result.AdminToolResult(
                        fields.contains(com.jeepclub.backend.tools.core.application.query.AdminToolField.ID)?t.get("id",Long.class):null,
                        fields.contains(com.jeepclub.backend.tools.core.application.query.AdminToolField.NAME)?t.get("name",String.class):null,
                        fields.contains(com.jeepclub.backend.tools.core.application.query.AdminToolField.STATUS)?t.get("status",ToolStatus.class):null,
                        fields.contains(com.jeepclub.backend.tools.core.application.query.AdminToolField.USER_ID)?t.get("userId",Long.class):null)).toList();
        var count = cb.createQuery(Long.class); var counted = count.from(ToolEntity.class);
        count.select(cb.count(counted)); count.where(specification.toPredicate(counted,count,cb));
        return new org.springframework.data.domain.PageImpl<>(content,stable,entityManager.createQuery(count).getSingleResult());
    }

    @Override
    public Page<Tool> findAll(String name, ToolStatus status, Pageable pageable) {
        return jpaRepository.findAll(ToolSpecifications.withFilters(name, status), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Tool> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Tool save(Tool tool) {
        var entity = mapper.toEntity(tool);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void delete(
            Tool tool,
            Long deletedByUserId,
            LocalDateTime deletedAt
    ) {
        findByIdForUpdate(tool.getId()).orElseThrow(() -> new ToolAlreadyDeletedException(tool.getId()));
        ToolEntity entity = jpaRepository
                .findByIdForUpdate(tool.getId())
                .orElseThrow(
                        () -> new ToolAlreadyDeletedException(
                                tool.getId()
                        )
                );

        ToolHistoryEntity history = historyMapper.toHistoryEntity(
                entity,
                deletedByUserId,
                deletedAt
        );

        historyJpaRepository.save(history);
        jpaRepository.delete(entity);
    }
    @Override
    public Optional<Tool> findByIdForUpdate(Long id) {
        var locked = jpaRepository.findByIdForUpdate(id);
        // Retain the DB lock while discarding any pre-lock ORM snapshot (MySQL REPEATABLE READ).
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpaRepository.findByIdForUpdate(id)).map(mapper::toDomain);
    }
}
