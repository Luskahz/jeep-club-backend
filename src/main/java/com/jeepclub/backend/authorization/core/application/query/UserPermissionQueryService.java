package com.jeepclub.backend.authorization.core.application.query;

import com.jeepclub.backend.authorization.core.repository.UserPermissionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPermissionQueryService {

    private final UserPermissionQueryRepository userPermissionQueryRepository;

    @Transactional(readOnly = true)
    public List<String> findPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        return userPermissionQueryRepository.findPermissionCodesByUserId(userId);
    }
}
