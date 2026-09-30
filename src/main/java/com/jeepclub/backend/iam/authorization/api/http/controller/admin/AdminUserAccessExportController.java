package com.jeepclub.backend.iam.authorization.api.http.controller.admin;
import com.jeepclub.backend.iam.authorization.core.application.service.export.UserAccessExportService;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.*;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
public class AdminUserAccessExportController {
    private final UserAccessExportService service;
    @GetMapping(value="/authorization/admin/users/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('AUTHORIZATION_EXPORT')") @RequiredPermission("AUTHORIZATION_EXPORT")
    @Operation(summary="Exportar usuários e acessos efetivos",description="Todos os usuários, inclusive sem papéis, ou um userId. Identificação via Identity e permissions distintas das roles ACTIVE. Inclui metadados dos vínculos e roles inativas sem conceder suas permissions.") @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(required=false) @Positive Long userId,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(userId,format));
    }
}
