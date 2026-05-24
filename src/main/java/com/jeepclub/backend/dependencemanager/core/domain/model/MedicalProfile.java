package com.jeepclub.backend.dependencemanager.core.domain.model;

import lombok.Getter;

import java.util.Objects;

@Getter
public class MedicalProfile {

    private final String bloodType;
    private final String allergies;
    private final String chronicDiseases;
    private final String medications;
    private final String medicalNotes;

    public MedicalProfile(
            String bloodType,
            String allergies,
            String chronicDiseases,
            String medications,
            String medicalNotes
    ) {
        this.bloodType = normalize(bloodType);
        this.allergies = normalize(allergies);
        this.chronicDiseases = normalize(chronicDiseases);
        this.medications = normalize(medications);
        this.medicalNotes = normalize(medicalNotes);
    }

    public static MedicalProfile empty() {
        return new MedicalProfile(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return bloodType == null &&
               allergies == null &&
               chronicDiseases == null &&
               medications == null &&
               medicalNotes == null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalProfile that = (MedicalProfile) o;
        return Objects.equals(bloodType, that.bloodType) &&
               Objects.equals(allergies, that.allergies) &&
               Objects.equals(chronicDiseases, that.chronicDiseases) &&
               Objects.equals(medications, that.medications) &&
               Objects.equals(medicalNotes, that.medicalNotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bloodType, allergies, chronicDiseases, medications, medicalNotes);
    }
}

