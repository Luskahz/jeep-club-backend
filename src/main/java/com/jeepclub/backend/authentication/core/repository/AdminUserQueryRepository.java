package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.Optional;

public interface AdminUserQueryRepository {

    Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    );

    Optional<AdminUserResult> findById(Long userId);
}
