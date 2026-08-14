package com.jeepclub.backend.dependents.core.port;

public record DependentMedicalProfileData(
        String bloodType,
        String allergies,
        String chronicDiseases,
        String medications,
        String medicalNotes
) {

    public boolean hasAnyValue() {
        return hasText(bloodType)
                || hasText(allergies)
                || hasText(chronicDiseases)
                || hasText(medications)
                || hasText(medicalNotes);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
