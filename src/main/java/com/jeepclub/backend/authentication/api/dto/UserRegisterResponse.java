package com.jeepclub.backend.authentication.api.dto;

import com.jeepclub.backend.authentication.core.domain.model.User;
import java.time.Instant;

/**
 * Data Transfer Object (DTO) de Resposta (Saída).
 * Limita quais dados do domínio interno irão trafegar para a internet (Esconde hash de senha, e dados sensíveis).
 */
public record UserRegisterResponse(
    Long id,
    String name,
    String cpf,
    String email,
    String status,
    Instant createdAt
) {
    public static UserRegisterResponse fromDomain(User user) {
        return new UserRegisterResponse(
                user.getId(),
                user.getName(),
                user.getCpf(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
