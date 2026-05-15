package com.jeepclub.backend.authorization.core.repository;

import java.util.List;

public interface UserPermissionQueryRepository {

    List<String> findPermissionCodesByUserId(Long userId);
}