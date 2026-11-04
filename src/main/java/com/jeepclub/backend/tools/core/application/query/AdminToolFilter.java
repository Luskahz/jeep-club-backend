package com.jeepclub.backend.tools.core.application.query;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;

public record AdminToolFilter(String q, String name, ToolStatus status, Long userId) {
    public AdminToolFilter {
        q = trim(q); name = trim(name);
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
