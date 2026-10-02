package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserProfileQuery;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.domain.model.ResidentialAddress;
import com.jeepclub.backend.iam.identity.core.domain.model.UserProfile;
import com.jeepclub.backend.iam.identity.core.domain.model.WorkProfile;
import com.jeepclub.backend.iam.identity.core.repository.UserProfileRepository;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService implements UserProfileQuery {
    private final UserRepository users;
    private final UserProfileRepository profiles;

    @Transactional(readOnly = true)
    public UserProfile get(Long userId) {
        if (!users.existsById(userId)) throw new UserNotFoundException(userId);
        return profiles.findByUserId(userId).orElseGet(() -> new UserProfile(userId, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingProfileCompletion(Long userId) {
        return get(userId).profileCompletionPending();
    }

    @Transactional
    public UserProfile replace(Long userId, WorkProfile workProfile, ResidentialAddress address) {
        // Lock the parent even before the first insert, serializing concurrent upserts.
        users.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return profiles.save(new UserProfile(userId, workProfile, address));
    }
}
