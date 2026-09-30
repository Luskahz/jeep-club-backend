package com.jeepclub.backend.iam.identity.core.repository;

import com.jeepclub.backend.iam.identity.core.domain.model.UserProfile;
import java.util.Optional;

public interface UserProfileRepository {
    Optional<UserProfile> findByUserId(Long userId);
    UserProfile save(UserProfile profile);
}
