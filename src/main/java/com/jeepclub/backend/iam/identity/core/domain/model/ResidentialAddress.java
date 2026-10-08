package com.jeepclub.backend.iam.identity.core.domain.model;

import java.util.Locale;
import java.util.Set;

public record ResidentialAddress(String postalCode, String street, String number,
                                 String complement, String neighborhood, String city, String state) {
    private static final Set<String> STATES = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    public ResidentialAddress {
        postalCode = UserProfile.normalize(postalCode, "postalCode", 10);
        if (postalCode != null) {
            if (!postalCode.matches("[0-9]{5}-?[0-9]{3}")) {
                throw new IllegalArgumentException("postalCode must be an eight-digit CEP, optionally with a hyphen");
            }
            postalCode = postalCode.replace("-", "");
        }
        street = UserProfile.normalize(street, "street", 150);
        number = UserProfile.normalize(number, "number", 20);
        complement = UserProfile.normalize(complement, "complement", 150);
        neighborhood = UserProfile.normalize(neighborhood, "neighborhood", 100);
        city = UserProfile.normalize(city, "city", 100);
        state = UserProfile.normalize(state, "state", 2);
        if (state != null) {
            state = state.toUpperCase(Locale.ROOT);
            if (!STATES.contains(state)) throw new IllegalArgumentException("state must be a Brazilian UF");
        }
    }

    public boolean isComplete() {
        return postalCode != null && street != null && number != null
                && neighborhood != null && city != null && state != null;
    }
}
