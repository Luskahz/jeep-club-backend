package com.jeepclub.backend.authentication.core.application.results.login;

public sealed interface LoginResult permits
        AuthenticatedLoginResult,
        PasswordChangeRequiredLoginResult {
}