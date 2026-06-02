package com.jeepclub.backend.membership.core.repository;

import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;

import java.util.Optional;

public interface MemberActivationTokenRepository {

    MemberActivationToken save(MemberActivationToken token);

    Optional<MemberActivationToken> findByTokenHash(String tokenHash);

    Optional<MemberActivationToken> findLatestByApplicationId(Long applicationId);

    void invalidateAllByApplicationId(Long applicationId);
}