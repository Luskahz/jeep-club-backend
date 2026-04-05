package com.jeepclub.backend.authentication.core.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidade de Domínio Puro (Hexagonal Architecture).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

}
