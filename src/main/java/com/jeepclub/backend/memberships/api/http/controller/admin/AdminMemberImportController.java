package com.jeepclub.backend.memberships.api.http.controller.admin;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;
import com.jeepclub.backend.memberships.api.http.dto.member.MemberRegistryRequests;
import com.jeepclub.backend.memberships.api.http.dto.member.MemberImportReportDTO;
import com.jeepclub.backend.memberships.core.application.service.member.MemberRegistryImportService;
@RestController @RequestMapping(value="/admin/members/imports",produces=MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor @Tag(name="Membership Registry")
@PreAuthorize("hasAuthority('MEMBERSHIP_MEMBER_REGISTRATION_MANAGE')")
@RequiredPermission("MEMBERSHIP_MEMBER_REGISTRATION_MANAGE")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Entrada inválida.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="401",description="Não autenticado.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="403",description="Permission ausente.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="404",description="Registro ou Identity não encontrado.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="409",description="Matrícula, vínculo, versão ou faixa em conflito.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class)))
})
public class AdminMemberImportController {
    @ModelAttribute
    void preventCaching(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    private final MemberRegistryImportService service;
    @PostMapping("/dry-run") @Operation(summary="Simular conciliação histórica sem escrita",description="Até 500 vínculos explicitamente verificados. Retorna índices e outcomes sanitizados e comprovante válido por 15 minutos, para este administrador e fonte exata.")
    @ApiResponse(responseCode="200",description="Simulação sem escrita e comprovante.")
    public MemberImportReportDTO dryRun(@Valid @RequestBody MemberRegistryRequests.ImportRequest request,@AuthenticationPrincipal UserPrincipal actor) {
        return MemberImportReportDTO.from(service.dryRun(request.rows().stream().map(MemberRegistryRequests.ImportRow::command).toList(),actor.getUserId(),request.reason()));
    }
    @PostMapping("/apply") @Operation(summary="Aplicar conciliação previamente simulada",description="Exige dryRunToken da mesma fonte, motivo e ator. Revalida conflitos. Cada vínculo é confirmado em transação independente com auditoria. Reexecutar pendências requer novo dry-run.")
    @ApiResponse(responseCode="200",description="Relatório por índice, incluindo aplicação parcial, conflitos e vínculos idempotentes.")
    public MemberImportReportDTO apply(@Valid @RequestBody MemberRegistryRequests.ImportRequest request,@AuthenticationPrincipal UserPrincipal actor) {
        return MemberImportReportDTO.from(service.apply(request.rows().stream().map(MemberRegistryRequests.ImportRow::command).toList(),actor.getUserId(),request.reason(),request.dryRunToken()));
    }
}
