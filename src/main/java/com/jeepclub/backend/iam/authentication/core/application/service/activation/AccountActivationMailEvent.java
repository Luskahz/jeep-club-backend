package com.jeepclub.backend.iam.authentication.core.application.service.activation;

/** Transient delivery intent; raw token is never persisted or logged. */
public record AccountActivationMailEvent(Long userId, String email, String name,
                                         String rawToken, String tokenHash) {
}
