package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;

import java.time.Instant;

public class MembershipApplication {

    private Long id;
    private String name;
    private String cpf;
    private String email;
    private String phoneNumber;
    private String message;
    private MembershipApplicationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private MembershipApplication(
            Long id,
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            MembershipApplicationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = requireText(name, "Nome");
        this.cpf = requireText(cpf, "CPF");
        this.email = requireText(email, "E-mail");
        this.phoneNumber = requireText(phoneNumber, "Telefone");
        this.message = requireText(message, "Mensagem");
        this.status = requireStatus(status);
        this.createdAt = requireDate(createdAt, "Data de criação");
        this.updatedAt = requireDate(updatedAt, "Data de atualização");
    }

    public static MembershipApplication create(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            Instant now
    ) {
        return new MembershipApplication(
                null,
                name,
                cpf,
                email,
                phoneNumber,
                message,
                MembershipApplicationStatus.PENDING_ACTIVATION,
                now,
                now
        );
    }

    public static MembershipApplication reconstitute(
            Long id,
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            MembershipApplicationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new MembershipApplication(
                id,
                name,
                cpf,
                email,
                phoneNumber,
                message,
                status,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório.");
        }

        return value.trim();
    }

    private static MembershipApplicationStatus requireStatus(MembershipApplicationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status da candidatura é obrigatório.");
        }

        return status;
    }

    private static Instant requireDate(Instant date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " é obrigatória.");
        }

        return date;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public MembershipApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}