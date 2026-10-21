package com.jeepclub.backend.memberships.api.http.dto.member;
import com.jeepclub.backend.memberships.core.domain.model.MemberRegistryAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
@Schema(name="MemberRegistryAuditResponse",description="Evento persistido e imutável de matrícula. Somente administradores autorizados.")
public record MemberAuditResponseDTO(Long id,Long memberId,String previousNumber,String newNumber,Long actorUserId,
        Instant occurredAt,String reason,String operation,String outcome) {
    public static MemberAuditResponseDTO from(MemberRegistryAudit a) {
        return new MemberAuditResponseDTO(a.id(),a.memberId(),a.previousNumber(),a.newNumber(),a.actorUserId(),a.occurredAt(),a.reason(),a.operation(),a.outcome());
    }
}
