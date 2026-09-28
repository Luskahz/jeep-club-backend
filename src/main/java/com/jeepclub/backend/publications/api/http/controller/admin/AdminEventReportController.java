package com.jeepclub.backend.publications.api.http.controller.admin;
import com.jeepclub.backend.publications.core.application.service.EventReportService;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.*;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
public class AdminEventReportController {
    private final EventReportService service;
    @GetMapping(value="/admin/events/{eventId}/reports/manifest/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório manifesto do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> manifest(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.MANIFEST,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/reports/transport/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório transporte do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> transport(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.TRANSPORT,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/reports/financial/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório financeiro do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> financial(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.FINANCIAL,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/reports/access/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório controle de acesso do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> access(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.ACCESS,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/reports/health-coverage/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório cobertura médica do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> health_coverage(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.HEALTH_COVERAGE,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/reports/post-event/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar relatório pós-evento do evento",description="Relatório administrativo CSV/PDF. Sem paginação HTTP. status seleciona situação da inscrição; type=MEMBER/DEPENDENT/GUEST; withVehicle seleciona alocação. view aplica-se somente a transporte. Financeiro aceita somente status; pós-evento não aceita filtros de participantes. Transporte filtra veículos pelos participantes selecionados, preservando sua ocupação real. Limite: 2.000 registros e vínculos operacionais do evento e 20.000 fatos financeiros antes da composição. Controle de acesso não contém clínica ou financeiro; cobertura informa apenas existência da ficha.") @ExportResponse
    public ResponseEntity<byte[]> post_event(@PathVariable @Positive Long eventId,@RequestParam(defaultValue="CSV") ExportFormat format,
            @RequestParam(required=false) String status,@RequestParam(required=false) String type,@RequestParam(required=false) Boolean withVehicle,
            @RequestParam(defaultValue="ALL") EventReportService.TransportView view) {
        return ExportDownload.response(service.export(eventId,EventReportService.Product.POST_EVENT,format,status,type,withVehicle,view));
    }
    @GetMapping(value="/admin/events/{eventId}/health/{type}/{target}/export",produces="application/pdf")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ')") @RequiredPermission("PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ")
    @Operation(summary="Exportar ficha médica emergencial individual",description="PDF individual. Exige Event válido, participante confirmado e ownership válido para dependente. Lifecycle não bloqueia acesso. Auditoria síncrona obrigatória antes de liberar dados. Não existe export clínico em massa.") @EmergencyPdfResponse
    public ResponseEntity<byte[]> emergency(@PathVariable @Positive Long eventId,@PathVariable MedicalProfileOwner type,@PathVariable @Positive Long target,@AuthenticationPrincipal UserPrincipal principal) {
        return ExportDownload.response(service.emergency(eventId,type,target,principal.getUserId()));
    }
}
