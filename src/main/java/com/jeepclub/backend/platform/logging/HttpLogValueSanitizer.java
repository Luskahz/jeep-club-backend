package com.jeepclub.backend.platform.logging;

final class HttpLogValueSanitizer {

    private HttpLogValueSanitizer() {
    }

    static String singleLine(String value) {
        return value.replaceAll("[\\p{Cntrl}]", "_");
    }

    static String quoted(String value) {
        return singleLine(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
