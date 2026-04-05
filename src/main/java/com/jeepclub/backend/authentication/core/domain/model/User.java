package com.jeepclub.backend.authentication.core.domain.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidade de Domínio Puro (Hexagonal Architecture).
 */
public class User {

    private Long id;
    private String name;
    private LocalDate birthData;
    private String email;
    private String cpf;
    private String rg;
    private String passwordHash;
    private String phoneNumber;
    private String profilePhotoUrl;
    private String status;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant disabledAt;
    private Instant updatedAt;

    public User() {
    }

    private User(Long id, String name, LocalDate birthData, String email, String cpf, String rg,
            String passwordHash, String phoneNumber, String profilePhotoUrl, String status,
            Instant lastLoginAt, Instant createdAt, Instant disabledAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.birthData = birthData;
        this.email = email;
        this.cpf = cpf;
        this.rg = rg;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.profilePhotoUrl = profilePhotoUrl;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.disabledAt = disabledAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method para criar um NOVO usuário contendo a regra inicial
     */
    public static User create(String name, LocalDate birthData, String email, String cpf, String rg,
            String passwordHash, String phoneNumber) {
        Instant now = Instant.now();
        // Assume "ACTIVE" como default para status recém criados (ajuste como preferir)
        return new User(null, name, birthData, email, cpf, rg, passwordHash, phoneNumber,
                null, "ACTIVE", null, now, null, now);
    }

    /**
     * Factory method para RECONSTITUIR um usuário existente através da
     * infraestrutura (banco de dados)
     */
    public static User reconstitute(Long id, String name, LocalDate birthData, String email, String cpf, String rg,
            String passwordHash, String phoneNumber, String profilePhotoUrl, String status,
            Instant lastLoginAt, Instant createdAt, Instant disabledAt, Instant updatedAt) {
        return new User(id, name, birthData, email, cpf, rg, passwordHash, phoneNumber,
                profilePhotoUrl, status, lastLoginAt, createdAt, disabledAt, updatedAt);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthData() {
        return birthData;
    }

    public void setBirthData(LocalDate birthData) {
        this.birthData = birthData;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Instant disabledAt) {
        this.disabledAt = disabledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
