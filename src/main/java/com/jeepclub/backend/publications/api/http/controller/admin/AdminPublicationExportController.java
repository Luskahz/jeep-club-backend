package com.jeepclub.backend.publications.api.http.controller.admin;
import com.jeepclub.backend.publications.core.application.service.PublicationExportService;
import com.jeepclub.backend.publications.core.repository.PublicationExportQuery.Product;
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
public class AdminPublicationExportController {
    private final PublicationExportService service;
    @GetMapping(value="/admin/notices/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar avisos",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> notices(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to) { return ExportDownload.response(service.export(Product.NOTICES,id,status,null,from,to,format)); }
    @GetMapping(value="/admin/notices/history/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar histórico de avisos",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> notice_history(@RequestParam(defaultValue="CSV") ExportFormat format) { return ExportDownload.response(service.export(Product.NOTICE_HISTORY,null,null,null,null,null,format)); }
    @GetMapping(value="/admin/services/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar serviços",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> services(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to) { return ExportDownload.response(service.export(Product.SERVICES,id,status,null,from,to,format)); }
    @GetMapping(value="/admin/services/history/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar histórico de serviços",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> service_history(@RequestParam(defaultValue="CSV") ExportFormat format) { return ExportDownload.response(service.export(Product.SERVICE_HISTORY,null,null,null,null,null,format)); }
    @GetMapping(value="/admin/events/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar eventos",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> events(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to, @RequestParam(required=false) String lifecycle) { return ExportDownload.response(service.export(Product.EVENTS,id,status,lifecycle,from,to,format)); }
    @GetMapping(value="/admin/events/history/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar histórico de eventos",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> event_history(@RequestParam(defaultValue="CSV") ExportFormat format) { return ExportDownload.response(service.export(Product.EVENT_HISTORY,null,null,null,null,null,format)); }
    @GetMapping(value="/admin/service-publication-requests/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar solicitações de serviço",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> service_requests(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to) { return ExportDownload.response(service.export(Product.SERVICE_REQUESTS,id,status,null,from,to,format)); }
    @GetMapping(value="/admin/service-publication-change-requests/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('PUBLICATIONS_EXPORT')") @RequiredPermission("PUBLICATIONS_EXPORT")
    @Operation(summary="Exportar solicitações de alteração de serviço",description="Todo o conjunto, sem paginação HTTP. Histórico separado. Filtros combináveis por ID, status e período quando disponíveis. Período: início do Event, solicitação de request ou criação de publicação. Conteúdo sem storage keys.") @ExportResponse
    public ResponseEntity<byte[]> change_requests(@RequestParam(defaultValue="CSV") ExportFormat format, @RequestParam(required=false) @Positive Long id, @RequestParam(required=false) String status, @RequestParam(required=false) java.time.Instant from, @RequestParam(required=false) java.time.Instant to) { return ExportDownload.response(service.export(Product.CHANGE_REQUESTS,id,status,null,from,to,format)); }
}
