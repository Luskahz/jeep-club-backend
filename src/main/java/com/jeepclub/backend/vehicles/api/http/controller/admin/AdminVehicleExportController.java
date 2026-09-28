package com.jeepclub.backend.vehicles.api.http.controller.admin;
import com.jeepclub.backend.vehicles.core.application.service.export.VehicleExportService;
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
@RestController @RequiredArgsConstructor @Tag(name="Veículos - Exportações")
public class AdminVehicleExportController {
    private final VehicleExportService service;
    @GetMapping(value="/vehicles/admin/export", produces={"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_EXPORT')") @RequiredPermission("VEHICLES_VEHICLE_EXPORT")
    @Operation(summary="Exportar veículos",description="Exporta todo o conjunto correspondente aos filtros, sem paginação HTTP. Sem filtros, exporta todos. ID seleciona um registro. Arquivo attachment sem dados pessoais no nome. Datas em America/Sao_Paulo. Histórico separado do operacional.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) @Positive Long ownerId) {
        return ExportDownload.response(service.export(format, id, ownerId, null, null, null, null));
    }
}
