package com.jeepclub.backend.memberships.api.http.controller.admin;
import com.jeepclub.backend.memberships.core.application.service.export.MembershipApplicationExportService;
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
@RestController @RequiredArgsConstructor @Tag(name="Solicitações de adesão - Exportações")
public class AdminMembershipApplicationExportController {
    private final MembershipApplicationExportService service;
    @GetMapping(value="/admin/membership-applications/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('MEMBERSHIP_EXPORT')") @RequiredPermission("MEMBERSHIP_EXPORT")
    @Operation(summary="Exportar solicitações de adesão",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String name, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to) {
        return ExportDownload.response(service.export(format, id, null, name, status, from, to));
    }
}
