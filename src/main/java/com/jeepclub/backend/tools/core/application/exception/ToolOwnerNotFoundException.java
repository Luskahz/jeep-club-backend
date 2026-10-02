package com.jeepclub.backend.tools.core.application.exception;

public class ToolOwnerNotFoundException extends RuntimeException {
    public ToolOwnerNotFoundException() {
        super("Proprietário da ferramenta não encontrado.");
    }
}
