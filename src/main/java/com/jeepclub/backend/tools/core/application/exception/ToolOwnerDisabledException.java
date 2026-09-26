package com.jeepclub.backend.tools.core.application.exception;

public class ToolOwnerDisabledException extends RuntimeException {
    public ToolOwnerDisabledException() {
        super("Proprietário da ferramenta está desabilitado.");
    }
}
