package com.jeepclub.backend.authentication.core.application.result.login;

public sealed interface LoginResult permits
        AuthenticatedLoginResult,
        PasswordChangeRequiredLoginResult {
}