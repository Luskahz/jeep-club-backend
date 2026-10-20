package com.jeepclub.backend.iam.identity.api.http.controller.admin;
import com.jeepclub.backend.iam.identity.core.application.service.history.UserDeletionHistoryService;
import com.jeepclub.backend.iam.identity.core.repository.UserDeletionHistoryQueryRepository;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.constraints.Positive;
@RestController @org.springframework.validation.annotation.Validated @lombok.RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="User - Deletion history")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Usuário não autenticado.",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/problem+json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.jeepclub.backend.platform.web.exception.ApiErrorResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Permissão de histórico ausente.",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/problem+json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.jeepclub.backend.platform.web.exception.ApiErrorResponse.class)))
})
@RequestMapping("/identity/admin/users")
public class UserDeletionHistoryController {
    private final UserDeletionHistoryService service;
    @GetMapping(value="/{id}/deletion-history",produces=MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('IDENTITY_USER_HISTORY_READ')") @RequiredPermission("IDENTITY_USER_HISTORY_READ")
    @io.swagger.v3.oas.annotations.Operation(summary="Consultar histórico de exclusão de User",description="Metadados mínimos de exclusão de Identity, sem PII ou credenciais. Consulta administrativa auditada.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Histórico de exclusão preservado.",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=UserDeletionHistoryQueryRepository.UserDeletionHistorySnapshot.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Histórico inexistente.",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/problem+json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.jeepclub.backend.platform.web.exception.ApiErrorResponse.class)))
    public ResponseEntity<UserDeletionHistoryQueryRepository.UserDeletionHistorySnapshot> find(@PathVariable @Positive Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(id,principal.getUserId()));
    }
}
