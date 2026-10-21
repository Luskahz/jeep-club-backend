package com.jeepclub.backend.memberships.api.http.dto.member;
import com.jeepclub.backend.memberships.core.domain.model.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
@Schema(name="MemberRegistryResponse",description="IDs técnicos imutáveis e matrícula vigente do associado.")
public record MemberResponseDTO(Long id,Long identityUserId,
        @Schema(example="027",description="Matrícula vigente preservada literalmente.") String membershipNumber,
        Instant createdAt,Instant updatedAt,@Schema(description="Versão exigida para retificação.") Long version) {
    public static MemberResponseDTO from(Member m) { return new MemberResponseDTO(m.id(),m.identityUserId(),m.membershipNumber(),m.createdAt(),m.updatedAt(),m.version()); }
}
