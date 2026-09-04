package com.jeepclub.backend.authentication.core.application.service.security;

import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountAccessDeniedException;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccessTokenAuthenticationService {

    private final SessionRepository sessionRepository;
    private final AuthenticationAccountRepository accountRepository;
    private final UserQuery identityQuery;
    private final Clock clock;

    public void validate(Long userId, Long sessionId) {
        Instant now = Instant.now(clock);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found."));

        if (!session.getUserId().equals(userId)) {
            throw new SessionUserMismatchException("Session does not belong to this user.");
        }

        if (!session.isValid(now)) {
            throw new SessionInvalidException("Session invalid.");
        }

        AuthenticationAccount account = accountRepository.findByIdentityId(userId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException("Authentication account not found."));

        if (!identityQuery.isAdministrativelyActive(userId)
                || !account.isAuthenticationAllowed()) {
            throw new AuthenticationAccountAccessDeniedException("Authentication access is disabled.");
        }
    }
}
