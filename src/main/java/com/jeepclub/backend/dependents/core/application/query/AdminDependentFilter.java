package com.jeepclub.backend.dependents.core.application.query;

import com.jeepclub.backend.dependents.core.application.exception.InvalidDependentSearchException;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;

import java.util.regex.Pattern;

public record AdminDependentFilter(
        String q,
        String name,
        String cpf,
        Long userId,
        RelationshipType relationshipType,
        DependentStatus status
) {
    private static final Pattern NUMERIC_FORMATTED_QUERY = Pattern.compile("^[\\d\\s.\\-()+/]+$");
    private static final Pattern CPF_FILTER_PATTERN = Pattern.compile("^[\\d\\s.\\-]+$");

    public AdminDependentFilter {
        q = trim(q);
        name = trim(name);
        cpf = normalizeCpfFilter(cpf);
    }

    public static String extractNumericQueryIfValid(String value) {
        String text = trim(value);
        if (text == null || !NUMERIC_FORMATTED_QUERY.matcher(text).matches()) {
            return null;
        }
        String digits = text.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String normalizeCpfFilter(String value) {
        String text = trim(value);
        if (text == null) {
            return null;
        }
        if (!CPF_FILTER_PATTERN.matcher(text).matches()) {
            throw new InvalidDependentSearchException("CPF filter must contain only digits and formatting characters.");
        }
        String digits = text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new InvalidDependentSearchException("CPF filter must contain at least one digit.");
        }
        return digits;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
