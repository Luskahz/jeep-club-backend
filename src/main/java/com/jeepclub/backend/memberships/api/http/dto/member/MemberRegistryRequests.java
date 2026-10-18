package com.jeepclub.backend.memberships.api.http.dto.member;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import com.jeepclub.backend.memberships.core.application.result.MemberImportRow;
public final class MemberRegistryRequests {
    private MemberRegistryRequests() { }
    public record Assignment(@NotBlank @Size(max=64) @Schema(example="027") String membershipNumber,@NotBlank @Size(max=500) String reason) { }
    public record Correction(@NotBlank @Size(max=64) @Schema(example="037") String membershipNumber,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=500) String reason) { }
    public record Generation(@NotBlank @Size(max=500) String reason) { }
    public record ImportRow(@NotNull @Positive Long identityUserId,@NotBlank @Size(max=64) String membershipNumber,
            boolean verified,@NotBlank @Size(max=100) String verificationReference) {
        public MemberImportRow command() { return new MemberImportRow(identityUserId,membershipNumber,verified,verificationReference); }
    }
    public record ImportRequest(@NotEmpty @Size(max=500) List<@NotNull @Valid ImportRow> rows,
            @NotBlank @Size(max=300) String reason,@Size(max=200) String dryRunToken) { }
}
