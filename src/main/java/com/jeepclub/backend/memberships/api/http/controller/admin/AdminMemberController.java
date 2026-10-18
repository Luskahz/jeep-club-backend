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
import com.jeepclub.backend.memberships.api.http.dto.member.*;
import com.jeepclub.backend.memberships.core.application.service.member.AdminMemberService;
import com.jeepclub.backend.memberships.core.domain.model.MemberRegistryAudit;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import org.springframework.data.domain.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.SortDefault;

@RestController @RequestMapping(value="/admin/members",produces=MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor @Tag(name="Membership Registry",description="Gestão autorizada de matrículas, vínculos e histórico.")
@PreAuthorize("hasAuthority('MEMBERSHIP_MEMBER_REGISTRATION_MANAGE')")
@RequiredPermission("MEMBERSHIP_MEMBER_REGISTRATION_MANAGE")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Entrada inválida.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="401",description="Não autenticado.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="403",description="Permission ausente.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="404",description="Registro ou Identity não encontrado.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="409",description="Matrícula, vínculo, versão ou faixa em conflito.",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class)))
})
public class AdminMemberController {
    @ModelAttribute
    void preventCaching(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    private final AdminMemberService service;
    @GetMapping @Operation(summary="Listar registros de associados",description="Página zero-based. Ordenação por id, identityUserId, membershipNumber, createdAt ou updatedAt. Não inclui usuários ainda sem registro.")
    @ApiResponse(responseCode="200",description="Página de registros.")
    public PageResponse<MemberResponseDTO> list(@ParameterObject @SortDefault(sort="id") Pageable page) {
        checkPage(page); return PageResponse.from(service.list(page).map(MemberResponseDTO::from));
    }
    @GetMapping("/by-number") @Operation(summary="Pesquisar matrícula vigente",description="Busca literal na matrícula atual. Números antigos só aparecem no histórico autorizado.")
    @ApiResponse(responseCode="200",description="Registro encontrado.")
    public MemberResponseDTO byNumber(@RequestParam String membershipNumber) { return MemberResponseDTO.from(service.byNumber(membershipNumber)); }
    @GetMapping("/by-identity/{userId}") @Operation(summary="Consultar vínculo e matrícula de uma identidade")
    @ApiResponse(responseCode="200",description="Registro encontrado.")
    public MemberResponseDTO get(@PathVariable @Positive Long userId) { return MemberResponseDTO.from(service.byIdentity(userId)); }
    @PutMapping("/by-identity/{userId}") @Operation(summary="Vincular Identity a matrícula histórica",description="Atribuição explícita. Repetir a mesma matrícula vigente é idempotente. Identity desativada não recebe novo vínculo.")
    @ApiResponse(responseCode="200",description="Registro criado ou vínculo idêntico preservado.")
    public MemberResponseDTO assign(@PathVariable @Positive Long userId,@Valid @RequestBody MemberRegistryRequests.Assignment request,@AuthenticationPrincipal UserPrincipal actor) {
        return MemberResponseDTO.from(service.assign(userId,request.membershipNumber(),actor.getUserId(),request.reason()));
    }
    @PatchMapping("/by-identity/{userId}/membership-number") @Operation(summary="Retificar matrícula vigente",description="Exige versão e justificativa. Preserva IDs, mantém número antigo indisponível e acrescenta auditoria atômica. Não altera Identity, credenciais, roles ou Billing.")
    @ApiResponse(responseCode="200",description="Matrícula vigente e versão retornadas.")
    public MemberResponseDTO correct(@PathVariable @Positive Long userId,@Valid @RequestBody MemberRegistryRequests.Correction request,@AuthenticationPrincipal UserPrincipal actor) {
        return MemberResponseDTO.from(service.correct(userId,request.membershipNumber(),request.expectedVersion(),actor.getUserId(),request.reason()));
    }
    @GetMapping("/by-identity/{userId}/history") @Operation(summary="Consultar histórico de matrícula",description="Eventos confirmados em ordem decrescente de ID. Somente leitura autorizada.")
    @ApiResponse(responseCode="200",description="Página de auditoria.")
    public PageResponse<MemberAuditResponseDTO> history(@PathVariable @Positive Long userId,@ParameterObject Pageable page) {
        checkPage(page);return PageResponse.from(service.history(userId,page).map(MemberAuditResponseDTO::from));
    }
    @PostMapping("/by-identity/{userId}/generate") @Operation(summary="Emitir matrícula na faixa homologada",description="Só funciona quando emissão está explicitamente habilitada e configurada. Repetição para identidade já vinculada retorna o vínculo vigente.")
    @ApiResponse(responseCode="200",description="Matrícula emitida ou existente.")
    public MemberResponseDTO generate(@PathVariable @Positive Long userId,@Valid @RequestBody MemberRegistryRequests.Generation request,@AuthenticationPrincipal UserPrincipal actor) {
        return MemberResponseDTO.from(service.generate(userId,actor.getUserId(),request.reason()));
    }
    @PostMapping("/reservations") @Operation(summary="Reservar número permanentemente",description="Reserva não cria associado e impede emissão ou atribuição posterior. Revisão prévia obrigatória pelo administrador.")
    @ApiResponse(responseCode="201",description="Número reservado.")
    public ResponseEntity<Void> reserve(@Valid @RequestBody MemberRegistryRequests.Assignment request,@AuthenticationPrincipal UserPrincipal actor) {
        service.reserve(request.membershipNumber(),actor.getUserId(),request.reason());return ResponseEntity.status(201).build();
    }
    private void checkPage(Pageable page) {
        var allowed=java.util.Set.of("id","identityUserId","membershipNumber","createdAt","updatedAt");
        if(page.getPageSize()>100 || page.getSort().stream().anyMatch(o -> !allowed.contains(o.getProperty()))) throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
    }
}
