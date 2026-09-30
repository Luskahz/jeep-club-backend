package com.jeepclub.backend.iam.authentication.api.http.controller.admin;
import com.jeepclub.backend.iam.authentication.core.application.service.export.AuthenticationAccountExportService;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.*;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor @Tag(name="Contas de autenticação - Exportações")
public class AdminAuthenticationAccountExportController {
    private final AuthenticationAccountExportService service;
    @GetMapping(value="/authentication/admin/accounts/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('AUTHENTICATION_EXPORT')") @RequiredPermission("AUTHENTICATION_EXPORT")
    @Operation(summary="Exportar contas de autenticação",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long identityId) {
        return ExportDownload.response(service.export(format, identityId, null, null, null, null, null));
    }
}
