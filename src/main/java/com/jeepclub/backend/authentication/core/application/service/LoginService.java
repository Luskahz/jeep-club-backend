package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.LoginResult;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordChangeChallengeIssuer challengeIssuer;
    private final TokenIssuanceService tokenIssuanceService;
    private final Clock clock;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResult login(String cpf, String password) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByCpfForUpdate(cpf)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.assertCanAttemptLogin();
        if (user.isChangePasswordRequired()) {
            return challengeIssuer.issue(user.getId(), now);
        }

        AuthTokens tokens = tokenIssuanceService.issue(user, now);
        user.recordSuccessfulLogin(now);
        userRepository.save(user);
        return new AuthenticatedLoginResult(tokens);
    }
}
