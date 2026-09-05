package com.jeepclub.backend.iam.authentication.core.application.result.login;

public sealed interface LoginResult permits
        AuthenticatedLoginResult,
        PasswordChangeRequiredLoginResult {
}