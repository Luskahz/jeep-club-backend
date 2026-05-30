package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class Dependent {

    private Long id;
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private RelationshipType relationshipType;
    private String phoneNumber;
    // não, o medical profile vc n precisa se preocurar, tirar daqui essa logica, o kauan está fazendo.
    private MedicalProfile medicalProfile;
    private boolean consentAccepted;
    private Instant consentAcceptedAt;
    private Long socioId;
    private Instant createdAt;
    private Instant updatedAt;
    // implementar logica de selfDelete criar campos status com ennum para active e deleted, criar deletedAt e implementar no fluxo
    // deleted deve ser implementado via entoint.

    private Dependent(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Instant consentAcceptedAt,
            Long socioId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = validateRequiredText(name, "Nome do dependente");
        this.cpf = validateCpf(cpf);
        this.birthDate = birthDate;
        this.relationshipType = validateRequired(relationshipType, "Tipo de parentesco");
        this.phoneNumber = normalizeNumber(phoneNumber);
        this.medicalProfile = medicalProfile == null ? MedicalProfile.empty() : medicalProfile;
        this.consentAccepted = validateConsent(consentAccepted);
        this.consentAcceptedAt = consentAcceptedAt;
        this.socioId = validateRequired(socioId, "ID do Sócio");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Dependent create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Long socioId,
            Instant now
    ) {
        Instant consentAt = consentAccepted ? now : null;
        return new Dependent(
                null,
                name,
                cpf,
                birthDate,
                relationshipType,
                phoneNumber,
                medicalProfile,
                consentAccepted,
                consentAt,
                socioId,
                now,
                now
        );
    }

    public static Dependent reconstitute(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Instant consentAcceptedAt,
            Long socioId,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Dependent(
                id,
                name,
                cpf,
                birthDate,
                relationshipType,
                phoneNumber,
                medicalProfile,
                consentAccepted,
                consentAcceptedAt,
                socioId,
                createdAt,
                updatedAt
        );
    }

    public void update(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Instant now
    ) {
        this.name = validateRequiredText(name, "Nome do dependente");
        this.cpf = validateCpf(cpf);
        this.birthDate = birthDate;
        this.relationshipType = validateRequired(relationshipType, "Tipo de parentesco");
        this.phoneNumber = normalizeNumber(phoneNumber);
        this.medicalProfile = medicalProfile == null ? MedicalProfile.empty() : medicalProfile;
        
        if (consentAccepted && !this.consentAccepted) {
            this.consentAccepted = true;
            this.consentAcceptedAt = now;
        } else if (!consentAccepted) {
            throw new DependentException("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.");
        }
        
        this.updatedAt = now;
    }

    private String validateRequiredText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new DependentException(fieldName + " é obrigatório.");
        }
        return text.trim();
    }

    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new DependentException(fieldName + " é obrigatório.");
        }
        return value;
    }

    private String validateCpf(String rawCpf) {
        if (rawCpf == null || rawCpf.isBlank()) {
            throw new DependentException("CPF é obrigatório.");
        }
        String cleanCpf = rawCpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            throw new DependentException("CPF deve conter exatamente 11 dígitos numéricos.");
        }
        return cleanCpf;
    }

    private boolean validateConsent(boolean consent) {
        if (!consent) {
            throw new DependentException("O consentimento de LGPD deve ser obrigatório para cadastro e manutenção de dependentes.");
        }
        return true;
    }

    private String normalizeNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.replaceAll("\\D", "");
    }
}

