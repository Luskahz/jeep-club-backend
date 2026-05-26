package com.jeepclub.backend.membership.api.dto;


// aplicar documentação swagger desta dto, usar o padrão do authentication/api/dto
public record RejectMembershipRequestDTO(
        String reason
) {}