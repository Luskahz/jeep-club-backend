package com.jeepclub.backend.dependents.core.domain.exception;

import lombok.Getter;

@Getter
public class DependentException extends RuntimeException {

    private final Violation violation;

    public DependentException(String message) {
        this(message, Violation.BUSINESS_RULE);
    }

    private DependentException(String message, Violation violation) {
        super(message);
        this.violation = violation;
    }

    public static DependentException notFound() {
        return new DependentException("Dependent not found.", Violation.NOT_FOUND);
    }

    public static DependentException accessDenied() {
        return new DependentException("Dependent access denied.", Violation.ACCESS_DENIED);
    }

    public static DependentException conflict() {
        return new DependentException("Dependent data conflict.", Violation.CONFLICT);
    }

    public enum Violation {
        BUSINESS_RULE,
        NOT_FOUND,
        ACCESS_DENIED,
        CONFLICT
    }
}
