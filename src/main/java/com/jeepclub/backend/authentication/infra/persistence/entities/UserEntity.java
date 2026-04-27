package com.jeepclub.backend.authentication.infra.persistence.entities;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidade de Banco de Dados.
 * Mapeamento ORM focado APENAS em como o Modelo se salva nas tabelas do SQL através do Hibernate.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_data")
    private LocalDate birthData;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(unique = true)
    private String rg;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "password_change_at")
    private Instant passwordChangeAt;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

}
