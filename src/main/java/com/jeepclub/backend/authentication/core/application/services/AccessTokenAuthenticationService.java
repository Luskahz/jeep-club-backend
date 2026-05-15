package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.TemQueVerISSO;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserDisabledException;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccessTokenAuthenticationService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public void validate(Long userId, Long sessionId) {
        Instant now = Instant.now();

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found."));

        if (!session.getUserId().equals(userId)) {
            throw new SessionUserMismatchException("Session does not belong to this user.");
        }

        if (!session.isValid(now)) {
            throw new IllegalArgumentException("Session invalid.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TemQueVerISSO("User not found."));

        if (!user.isActive()) {
            throw new UserDisabledException("User inactive.");
        }
    }
}