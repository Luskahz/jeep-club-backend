package com.jeepclub.backend.iam.authorization.api.http.controller.admin;
import com.jeepclub.backend.iam.authorization.core.application.service.export.PermissionExportService;
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
@RestController @RequiredArgsConstructor @Tag(name="Permissões - Exportações")
public class AdminPermissionExportController {
    private final PermissionExportService service;
    @GetMapping(value="/authorization/admin/permissions/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('AUTHORIZATION_EXPORT')") @RequiredPermission("AUTHORIZATION_EXPORT")
    @Operation(summary="Exportar permissões",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id) {
        return ExportDownload.response(service.export(format, id, null, null, null, null, null));
    }
}
