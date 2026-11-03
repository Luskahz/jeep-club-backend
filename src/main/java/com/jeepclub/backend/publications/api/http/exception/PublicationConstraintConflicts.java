package com.jeepclub.backend.publications.api.http.exception;

import org.hibernate.exception.ConstraintViolationException;
import java.util.Locale;
import java.util.Set;

/** Classify named uniqueness constraints only; generic SQL/FK failures remain server errors. */
final class PublicationConstraintConflicts {
    private PublicationConstraintConflicts() {}
    static boolean matches(Throwable failure, Set<String> known) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName().replace("\"", "").toLowerCase(Locale.ROOT);
                name = name.substring(name.lastIndexOf('.') + 1);
                for (String constraint : known)
                    if (name.equals(constraint) || name.startsWith(constraint + "_index_")) return true;
            }
            if (cause.getCause() == cause) break;
        }
        return false;
    }
}
