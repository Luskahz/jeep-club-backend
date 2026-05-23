package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public MeResult me(
            @NotNull Long userId,
            @NotNull Long sessionId,
            @NotNull Instant accessTokenExpiresAt
    ) {
        Instant now = Instant.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User not found"));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found."));

        if (!session.getUserId().equals(user.getId())) {
            throw new SessionUserMismatchException("Session does not belong to this user");
        }

        return new MeResult(
                user.getId(),
                session.getId(),
                session.isValid(now),
                getAccessTokenRemainingSeconds(now, accessTokenExpiresAt)
        );
    }

    private long getAccessTokenRemainingSeconds(
            Instant now,
            Instant accessTokenExpiresAt
    ) {
        return Math.max(
                Duration.between(now, accessTokenExpiresAt).getSeconds(),
                0
        );
    }
}