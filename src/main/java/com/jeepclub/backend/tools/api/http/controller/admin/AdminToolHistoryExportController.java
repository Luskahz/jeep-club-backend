package com.jeepclub.backend.tools.api.http.controller.admin;
import com.jeepclub.backend.tools.core.application.service.export.ToolHistoryExportService;
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
@RestController @RequiredArgsConstructor @Tag(name="Histórico de ferramentas - Exportações")
public class AdminToolHistoryExportController {
    private final ToolHistoryExportService service;
    @GetMapping(value="/admin/tools/history/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('TOOLS_TOOL_EXPORT')") @RequiredPermission("TOOLS_TOOL_EXPORT")
    @Operation(summary="Exportar histórico de ferramentas",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(format, null, null, null, null, null, null));
    }
}
