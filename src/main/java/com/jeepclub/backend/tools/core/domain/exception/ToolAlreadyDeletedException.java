package com.jeepclub.backend.tools.core.domain.exception;

public class ToolAlreadyDeletedException extends RuntimeException {

    public ToolAlreadyDeletedException(Long toolId) {
        super(
                "Tool with id "
                        + toolId
                        + " is already deleted."
        );
    }
}
