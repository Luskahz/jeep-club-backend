package com.jeepclub.backend.shared.export;
public class ExportException extends RuntimeException {
    public enum Reason { INVALID_FILTER, NOT_FOUND, LIMIT, GENERATION }
    private final Reason reason;
    public ExportException(Reason reason) { super("Exportação indisponível: " + reason); this.reason = reason; }
    public Reason reason() { return reason; }
}
