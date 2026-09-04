package com.jeepclub.backend.iam.authentication.core.application.result.login;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;

import java.util.Objects;

public record AuthenticatedLoginResult(
        AuthTokens tokens
) implements LoginResult {

    public AuthenticatedLoginResult {
        Objects.requireNonNull(tokens, "tokens cannot be null");
    }
}