package com.jeepclub.backend.identity.api.module;

public interface UserRegistration {

    UserAuthenticationTokens registerAndAuthenticate(
            UserRegistrationData data,
            String rawPassword
    );

    Long createWithPermanentCredential(
            UserRegistrationData data,
            String rawPassword
    );

    Long createPendingFirstAccess(
            UserRegistrationData data,
            String rawPassword
    );
}
