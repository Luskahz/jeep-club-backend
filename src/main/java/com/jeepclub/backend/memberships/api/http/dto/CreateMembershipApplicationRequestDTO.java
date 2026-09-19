package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Dados de uma solicitação pública de adesão ao clube.")
public record CreateMembershipApplicationRequestDTO(

        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        @Schema(description = "Nome completo do candidato.", example = "Marina da Silva", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        String name,

        @NotBlank(message = "CPF é obrigatório.")
        @Pattern(
                regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                message = "CPF deve estar no formato 00000000000 ou 000.000.000-00."
        )
        @CPF(message = "CPF inválido.")
        @Schema(description = "CPF com 11 dígitos, com ou sem pontuação.", example = "123.456.789-09", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$")
        String cpf,

        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        @Schema(description = "E-mail informado pelo candidato.", example = "marina.silva@example.com", nullable = true, maxLength = 180)
        String email,

        @NotBlank(message = "Telefone é obrigatório.")
        @Pattern(
                regexp = "^(?:\\+55\\s?)?(?:\\([1-9]{2}\\)|[1-9]{2})\\s?9\\d{4}[-\\s]?\\d{4}$",
                message = "Telefone deve ser um celular brasileiro com DDD."
        )
        @Schema(description = "Celular brasileiro com DDD, com ou sem formatação.", example = "+55 (11) 91234-5678", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^(?:\\+55\\s?)?(?:\\([1-9]{2}\\)|[1-9]{2})\\s?9\\d{4}[-\\s]?\\d{4}$")
        String phoneNumber,

        @Size(max = 2000, message = "Mensagem deve ter no máximo 2000 caracteres.")
        @Schema(description = "Mensagem opcional enviada com a solicitação.", example = "Gostaria de participar dos próximos encontros.", nullable = true, maxLength = 2000)
        String message
) {}
