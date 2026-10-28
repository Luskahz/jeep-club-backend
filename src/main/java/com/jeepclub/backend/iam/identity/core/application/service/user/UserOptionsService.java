package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.UserOptionResult;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserOptionsService {
    private final AdminUserQueryRepository users;

    @Transactional(readOnly = true)
    public Page<UserOptionResult> find(String name, int page, int size) {
        String normalizedName = name == null || name.isBlank() ? null : name.trim();
        var filter = new AdminUserFilter(null, normalizedName, null, null, null, null,
                null, UserStatus.ACTIVE, null, null, null, null, null);
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
        return users.findAll(filter, Set.of(AdminUserField.ID, AdminUserField.NAME), pageable)
                .map(user -> new UserOptionResult(user.id(), user.name()));
    }
}
