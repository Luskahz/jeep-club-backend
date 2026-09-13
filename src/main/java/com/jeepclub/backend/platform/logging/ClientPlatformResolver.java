package com.jeepclub.backend.platform.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public class ClientPlatformResolver {

    public static final String HEADER_NAME = "X-Client-Platform";
    public static final String REQUEST_ATTRIBUTE = ClientPlatformResolver.class.getName() + ".platform";
    private static final int MAX_HEADER_LENGTH = 16;

    public ClientPlatform resolve(HttpServletRequest request) {
        String supplied = request.getHeader(HEADER_NAME);
        if (supplied == null || supplied.isBlank() || supplied.length() > MAX_HEADER_LENGTH) {
            return ClientPlatform.UNKNOWN;
        }

        try {
            return ClientPlatform.valueOf(supplied.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ClientPlatform.UNKNOWN;
        }
    }
}
