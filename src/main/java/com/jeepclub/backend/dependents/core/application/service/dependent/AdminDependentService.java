package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.exception.InvalidDependentSearchException;
import com.jeepclub.backend.dependents.core.application.query.AdminDependentField;
import com.jeepclub.backend.dependents.core.application.query.AdminDependentFilter;
import com.jeepclub.backend.dependents.core.application.result.AdminDependentSearchResult;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminDependentService {

    private final DependentRepository dependentRepository;
    private final DependentUserPort userPort;

    public AdminDependentService(DependentRepository dependentRepository) {
        this(dependentRepository, null);
    }

    @Autowired
    public AdminDependentService(
            DependentRepository dependentRepository,
            DependentUserPort userPort
    ) {
        this.dependentRepository = dependentRepository;
        this.userPort = userPort;
    }

    @Transactional(readOnly = true)
    public Page<AdminDependentSearchResult> search(
            AdminDependentFilter filter,
            Set<AdminDependentField> fields,
            Pageable pageable
    ) {
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new InvalidDependentSearchException("Pagination offset exceeds the supported range.");
        }
        Page<AdminDependentSearchResult> page = dependentRepository.search(filter, fields, pageable);
        if (!fields.contains(AdminDependentField.OWNER_NAME) || page.isEmpty() || userPort == null) {
            return page;
        }
        List<Long> ownerIds = page.getContent().stream()
                .map(AdminDependentSearchResult::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> names = userPort.findNamesByIds(ownerIds);
        return page.map(row -> row.withOwnerName(names.get(row.userId())));
    }

    @Transactional(readOnly = true)
    public AdminDependentSearchResult findAdminById(Long id) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new DependentNotFoundException(id));
        String ownerName = userPort == null
                ? null
                : userPort.findNamesByIds(List.of(dependent.getUserId())).get(dependent.getUserId());
        return AdminDependentSearchResult.from(dependent, ownerName);
    }

    @Transactional(readOnly = true)
    public List<DependentResult> findAllByUserId(Long userId) {
        return dependentRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findByUserIdAndId(
            Long userId,
            Long id
    ) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );

        if (!dependent.getUserId().equals(userId)) {
            throw new DependentNotFoundException(id);
        }

        return toResult(dependent);
    }

    private DependentResult toResult(Dependent dependent) {
        return DependentResult.from(dependent);
    }
}
