package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidade de Domínio Puro (Hexagonal Architecture).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
    private UserStatus status;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant disabledAt;
    private Instant updatedAt;
    private Instant passwordChangedAt;
    private int failedLoginAttempts;

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;


    private User(String name, LocalDate birthData, String email, String cpf, String rg, String passwordHash, String phoneNumber, Instant now){
        this.name = name;
        this.birthData = birthData;
        this.email = email;
        this.cpf = cpf;
        this.rg = rg;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.status = UserStatus.ACTIVE;
        this.createdAt = now;
        this.failedLoginAttempts = 0;
    }


    @Contract("_, _, _ -> new")
    public static @NotNull User create(
            String name,
            LocalDate birthData,
            String email,
            String cpf,
            String rg,
            String passwordHash,
            String phoneNumber,
            Instant now
    ) {
        return new User(
                name,
                birthData,
                email,
                cpf,
                rg,
                passwordHash,
                phoneNumber,
                now
        );
    }

    /**
     * Factory method para RECONSTITUIR um usuário existente através da
     * infraestrutura (banco de dados)
     */
    public static @NonNull User reconstitute(
            Long id,
            String name,
            LocalDate birthData,
            String email,
            String cpf,
            String rg,
            String passwordHash,
            String phoneNumber,
            String profilePhotoUrl,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt,
            Instant disabledAt,
            Instant updatedAt,
            Instant passwordChangedAt,
            int failedLoginAttempts
    ) {
        User user = new User();
        user.id = id;
        user.name = name;
        user.birthData = birthData;
        user.email = email;
        user.cpf = cpf;
        user.rg = rg;
        user.passwordHash = passwordHash;
        user.phoneNumber = phoneNumber;
        user.profilePhotoUrl = profilePhotoUrl;
        user.status = status;
        user.lastLoginAt = lastLoginAt;
        user.createdAt = createdAt;
        user.disabledAt = disabledAt;
        user.updatedAt = updatedAt;
        user.passwordChangedAt = passwordChangedAt;
        user.failedLoginAttempts = failedLoginAttempts;
        return user;
    }


    public void registerFailedLogin() {
        if(isBlockedForLogin()){
            throw new IllegalStateException("User is not Available to login");
        }
        failedLoginAttempts++;

        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            status = UserStatus.LOCKED;
        }
    }
    public void changePassword(
            String newHash,
            Instant now
    ) {
        if (newHash == null || newHash.isBlank()) throw new IllegalArgumentException("newHash is required");
        if (now == null) throw new IllegalArgumentException("now is required");

        this.passwordHash = newHash;
        this.passwordChangedAt = now;
        this.failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin(Instant now) {
        this.lastLoginAt = now;
        this.failedLoginAttempts = 0;
        this.updatedAt = now;
    }


    public boolean isBlockedForLogin() {
        return isLocked() || isDisabled();
    }
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }
    public boolean isDisabled() {
        return status == UserStatus.DISABLED;
    }
    public void unlock() {
        if (status != UserStatus.LOCKED) throw new IllegalStateException("User is not locked");
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
    }
    public void reactivate() {
        if (status != UserStatus.DISABLED) throw new IllegalStateException("User is not inactive");
        this.status = UserStatus.ACTIVE;
    }
    public User assertCanAuthenticate() {
        if (isBlockedForLogin()) {
            throw new IllegalStateException("User can not authenticate");
        }
        return this;
    }

    public void assertCanRequestPasswordChange() {
        if (status == UserStatus.DISABLED) {
            throw new IllegalStateException("User cannot request password change");
        }
    }
}
