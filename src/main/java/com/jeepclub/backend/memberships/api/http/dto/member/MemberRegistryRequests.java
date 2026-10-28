package com.jeepclub.backend.memberships.api.http.dto.member;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import com.jeepclub.backend.memberships.core.application.result.MemberImportRow;
public final class MemberRegistryRequests {
    private MemberRegistryRequests() { }
    public record Assignment(@NotBlank @Size(max=64) @Schema(example="27",description="Decimal positivo até 64 dígitos; zeros iniciais são normalizados. Sem prefixos.") String membershipNumber,@NotBlank @Size(max=500) String reason) { }
    public record Correction(@NotBlank @Size(max=64) @Schema(example="37",description="Número vigente novo. A correção libera o anterior e preserva auditoria.") String membershipNumber,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=500) String reason) { }
    public record Generation(@NotBlank @Size(max=500) String reason) { }
    public record ImportRow(@NotNull @Positive Long identityUserId,@NotBlank @Size(max=64) String membershipNumber,
            @Schema(requiredMode=Schema.RequiredMode.REQUIRED, description="Declaração explícita de conferência humana com a fonte oficial. O backend só valida integridade técnica; não consulta cadastro externo.")
            boolean administrativelyConfirmed,
            @Schema(description="Referência opaca de origem/evidência conferida, sem CPF, e-mail ou outros dados pessoais.")
            @NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}") String sourceReference) {
        public MemberImportRow command() { return new MemberImportRow(identityUserId,membershipNumber,administrativelyConfirmed,sourceReference); }
    }
    public record ImportRequest(@NotEmpty @Size(max=500) List<@NotNull @Valid ImportRow> rows,
            @NotBlank @Size(max=300) String reason,@Size(max=200) String dryRunToken) { }
}
