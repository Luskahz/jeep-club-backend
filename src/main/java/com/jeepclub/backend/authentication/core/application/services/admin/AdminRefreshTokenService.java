package com.jeepclub.backend.authentication.core.application.services.admin;

import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RFNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.results.admin.refresh.AdminRefreshTokenResult;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminRefreshTokenResult> findAll() {
        return AdminRefreshTokenResult.from(refreshTokenRepository.findAll());
    }

    @Transactional(readOnly = true)
    public AdminRefreshTokenResult findById(Long refreshTokenId) {
        RefreshToken refreshToken = findRefreshTokenById(refreshTokenId);

        return AdminRefreshTokenResult.from(refreshToken);
    }

    @Transactional(readOnly = true)
    public List<AdminRefreshTokenResult> findByUserId(Long userId) {
        ensureUserExists(userId);

        return AdminRefreshTokenResult.from(
                refreshTokenRepository.findByUserId(userId)
        );
    }

    @Transactional
    public AdminRefreshTokenResult revoke(Long refreshTokenId) {
        Instant now = Instant.now(clock);

        RefreshToken refreshToken = refreshTokenRepository
                .findById(refreshTokenId)
                .orElseThrow(() ->
                        new RefreshTokenNotFoundException(
                                refreshTokenId
                        )
                );

        refreshToken.revoke(now);

        RefreshToken savedRefreshToken =
                refreshTokenRepository.save(refreshToken);

        return AdminRefreshTokenResult.from(
                savedRefreshToken
        );
    }

    private RefreshToken findRefreshTokenById(Long refreshTokenId) {
        return refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new RFNotFoundException(refreshTokenId));
    }

    private void ensureUserExists(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserIdNotFoundException(userId);
        }
    }
}