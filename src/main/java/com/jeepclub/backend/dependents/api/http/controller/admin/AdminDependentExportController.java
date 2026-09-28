package com.jeepclub.backend.dependents.api.http.controller.admin;
import com.jeepclub.backend.dependents.core.application.service.export.DependentExportService;
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
@RestController @RequiredArgsConstructor @Tag(name="Dependentes - Exportações")
public class AdminDependentExportController {
    private final DependentExportService service;
    @GetMapping(value="/admin/dependents/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_EXPORT')") @RequiredPermission("DEPENDENTS_DEPENDENT_EXPORT")
    @Operation(summary="Exportar dependentes",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) @Positive Long userId, @RequestParam(required=false) String name, @RequestParam(required=false) String status) {
        return ExportDownload.response(service.export(format, id, userId, name, status, null, null));
    }
}
