package com.jeepclub.backend.authentication.core.application.service.session;

import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.admin.session.AdminSessionResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final SessionRepository sessionRepository;
    private final IdentityQuery identityQuery;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminSessionResult> findAll() {
        return AdminSessionResult.from(
                sessionRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public AdminSessionResult findById(
            Long sessionId
    ) {
        Session session =
                findSessionById(sessionId);

        return AdminSessionResult.from(session);
    }

    @Transactional(readOnly = true)
    public List<AdminSessionResult> findByUserId(
            Long userId
    ) {
        ensureUserExists(userId);

        return AdminSessionResult.from(
                sessionRepository.findByUserId(userId)
        );
    }

    @Transactional
    public AdminSessionResult logout(
            Long sessionId
    ) {
        Session session =
                findSessionByIdForUpdate(sessionId);

        Instant now = Instant.now(clock);

        session.logout(now);

        Session savedSession =
                sessionRepository.save(session);

        return AdminSessionResult.from(
                savedSession
        );
    }

    private Session findSessionById(
            Long sessionId
    ) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(
                        () -> new SessionNotFoundException(
                                sessionId
                        )
                );
    }

    private Session findSessionByIdForUpdate(
            Long sessionId
    ) {
        return sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(
                        () -> new SessionNotFoundException(
                                sessionId
                        )
                );
    }

    private void ensureUserExists(Long userId) {
        if (!identityQuery.existsById(userId)) {
            throw new UserIdNotFoundException(userId);
        }
    }
}
