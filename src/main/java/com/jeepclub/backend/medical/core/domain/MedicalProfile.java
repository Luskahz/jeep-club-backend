package com.jeepclub.backend.medical.core.domain;

import lombok.Getter;

import java.time.Instant;

// fiz uma correção pois n me aguentei, use as tags lombok, @getters substitui todos aqueles getters no final do arquivo. tem @Setters tbm, mas eu n usaria sem pensar...estude sobre.
@Getter
public class MedicalProfile {

    private Long id;
    private MedicalProfileOwnerType ownerType;
    private Long ownerId;
    private BloodType bloodType;
    private String allergies;
    private String chronicConditions;
    private String continuousMedications;
    private String healthInsuranceProvider;
    private String healthInsurancePlan;
    private String healthInsuranceNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String observations;
    private Instant createdAt;
    private Instant updatedAt;

    public MedicalProfile(
            Long id,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            BloodType bloodType,
            String allergies,
            String chronicConditions,
            String continuousMedications,
            String healthInsuranceProvider,
            String healthInsurancePlan,
            String healthInsuranceNumber,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String observations,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (ownerType == null) {
            // faça exceptions personalizadas para o modelll!! em core.domain.exceptions inclua-as no handler em api.exception
            throw new IllegalArgumentException("O tipo do proprietário do perfil médico é obrigatório.");
        }

        if (ownerId == null) {
            throw new IllegalArgumentException("O identificador do proprietário do perfil médico é obrigatório.");
        }

        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.bloodType = bloodType == null ? BloodType.UNKNOWN : bloodType;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.continuousMedications = continuousMedications;
        this.healthInsuranceProvider = healthInsuranceProvider;
        this.healthInsurancePlan = healthInsurancePlan;
        this.healthInsuranceNumber = healthInsuranceNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.observations = observations;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // o padrão é construtor create e construtor reconstitute, quando vc atualiza um profile, vc n cria um novo, vc atualiza um que já existe.
    // vc faz MedicalProfile.create(dados) e salva ele no banco com o save do repository, depois quando vc tem que alterar
    // vc traz ele com o repository E USA O CONSTRUTOR RECONSTITUTE QUE RECONSTITUI A CLASSE Q JA EXISTE NO BANCO,
    // altera no service usando métodos internos da classe, e salva novamente no banco com o save, o jpa cuida do resto, estude sobre isso.
    public void update(
            BloodType bloodType,
            String allergies,
            String chronicConditions,
            String continuousMedications,
            String healthInsuranceProvider,
            String healthInsurancePlan,
            String healthInsuranceNumber,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String observations
    ) {
        this.bloodType = bloodType == null ? BloodType.UNKNOWN : bloodType;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.continuousMedications = continuousMedications;
        this.healthInsuranceProvider = healthInsuranceProvider;
        this.healthInsurancePlan = healthInsurancePlan;
        this.healthInsuranceNumber = healthInsuranceNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.observations = observations;
    }
    // faltando muitos metodos auxiliares, conforme for criando as rotas vai entender.
}