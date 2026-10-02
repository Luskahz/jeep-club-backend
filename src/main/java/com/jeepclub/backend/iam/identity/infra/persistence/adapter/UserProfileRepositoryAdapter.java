package com.jeepclub.backend.iam.identity.infra.persistence.adapter;

import com.jeepclub.backend.iam.identity.core.domain.model.UserProfile;
import com.jeepclub.backend.iam.identity.core.repository.UserProfileRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserProfileEntity;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserProfileJpaRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryAdapter implements UserProfileRepository {
    private final UserProfileJpaRepository profiles;
    private final UserJpaRepository users;
    private final UserProfileMapper mapper;

    @Override
    public Optional<UserProfile> findByUserId(Long userId) {
        return profiles.findById(userId).map(mapper::toDomain);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileEntity entity = profiles.findById(profile.userId()).orElseGet(() -> {
            UserProfileEntity created = new UserProfileEntity();
            created.setUser(users.getReferenceById(profile.userId()));
            return created;
        });
        mapper.copy(profile, entity);
        return mapper.toDomain(profiles.saveAndFlush(entity));
    }
}
