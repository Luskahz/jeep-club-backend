package com.jeepclub.backend.tools.core.application.result;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;

public record AdminToolResult(Long id, String name, ToolStatus status, Long userId) {}
