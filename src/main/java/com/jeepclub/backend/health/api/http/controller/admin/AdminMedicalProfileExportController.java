package com.jeepclub.backend.health.api.http.controller.admin;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileExportService;
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
public class AdminMedicalProfileExportController {
    private final MedicalProfileExportService service;
    @GetMapping(value="/admin/medical-profiles/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_EXPORT')") @RequiredPermission("HEALTH_MEDICAL_PROFILE_EXPORT")
    @Operation(summary="Exportar perfis médicos",description="Dados sensíveis. Todos os perfis operacionais de owners ativos ou seleção exclusiva por profileId, userId ou dependentId. household=true com userId inclui titular e todos os seus dependentes, mesmo sem ficha. CSV plano e PDF agrupado. Auditoria obrigatória sem dados clínicos, cache proibido.") @ExportResponse
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue="CSV") ExportFormat format,@RequestParam(required=false) @Positive Long profileId,
            @RequestParam(required=false) @Positive Long userId,@RequestParam(required=false) @Positive Long dependentId,@RequestParam(defaultValue="false") boolean household) {
        return ExportDownload.response(service.export(format,false,profileId,userId,dependentId,household));
    }
    @GetMapping(value="/admin/medical-profiles/history/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_EXPORT')") @RequiredPermission("HEALTH_MEDICAL_PROFILE_EXPORT")
    @Operation(summary="Exportar histórico de perfis médicos",description="Todos os snapshots de exclusão, separados dos perfis operacionais. Dados sensíveis com auditoria e cache proibido.") @ExportResponse
    public ResponseEntity<byte[]> history(@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(format,true,null,null,null,false));
    }
}
