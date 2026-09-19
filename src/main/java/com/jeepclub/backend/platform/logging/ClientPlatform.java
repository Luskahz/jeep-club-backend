package com.jeepclub.backend.platform.logging;

/**
 * Controlled, observational classification of the HTTP client platform.
 * This value must never participate in authentication or authorization.
 */
public enum ClientPlatform {
    WEB,
    ANDROID,
    IOS,
    UNKNOWN
}
