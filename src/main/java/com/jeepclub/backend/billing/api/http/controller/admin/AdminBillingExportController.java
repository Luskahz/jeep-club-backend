package com.jeepclub.backend.billing.api.http.controller.admin;
import com.jeepclub.backend.billing.core.application.service.export.BillingExportService;
import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery.Product;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.*;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
public class AdminBillingExportController {
    private final BillingExportService service;
    @GetMapping(value="/billing/admin/definitions/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('BILLING_EXPORT')") @RequiredPermission("BILLING_EXPORT")
    @Operation(summary="Exportar definições e atribuições",description="Todos, por ID ou filtros combinados. Ano/mês são derivados do vencimento do ciclo, e eventId do contexto estruturado. Pagamentos usam período de submissão; reembolsos usam solicitação; demais usam criação. Situação efetiva calculada com Clock. Limite de leitura: 100.000 registros candidatos. Sem chave técnica do comprovante. Arquivo completo ou 413.") @ExportResponse
    public ResponseEntity<byte[]> definitions(@ParameterObject @ModelAttribute BillingExportFilter filters,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(Product.DEFINITIONS,filters,format));
    }
    @GetMapping(value="/billing/admin/cycles/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('BILLING_EXPORT')") @RequiredPermission("BILLING_EXPORT")
    @Operation(summary="Exportar ciclos de cobrança",description="Todos, por ID ou filtros combinados. Ano/mês são derivados do vencimento do ciclo, e eventId do contexto estruturado. Pagamentos usam período de submissão; reembolsos usam solicitação; demais usam criação. Situação efetiva calculada com Clock. Limite de leitura: 100.000 registros candidatos. Sem chave técnica do comprovante. Arquivo completo ou 413.") @ExportResponse
    public ResponseEntity<byte[]> cycles(@ParameterObject @ModelAttribute BillingExportFilter filters,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(Product.CYCLES,filters,format));
    }
    @GetMapping(value="/billing/admin/member-charges/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('BILLING_EXPORT')") @RequiredPermission("BILLING_EXPORT")
    @Operation(summary="Exportar cobranças dos membros",description="Todos, por ID ou filtros combinados. Ano/mês são derivados do vencimento do ciclo, e eventId do contexto estruturado. Pagamentos usam período de submissão; reembolsos usam solicitação; demais usam criação. Situação efetiva calculada com Clock. Limite de leitura: 100.000 registros candidatos. Sem chave técnica do comprovante. Arquivo completo ou 413.") @ExportResponse
    public ResponseEntity<byte[]> charges(@ParameterObject @ModelAttribute BillingExportFilter filters,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(Product.CHARGES,filters,format));
    }
    @GetMapping(value="/billing/admin/payments/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('BILLING_EXPORT')") @RequiredPermission("BILLING_EXPORT")
    @Operation(summary="Exportar pagamentos",description="Todos, por ID ou filtros combinados. Ano/mês são derivados do vencimento do ciclo, e eventId do contexto estruturado. Pagamentos usam período de submissão; reembolsos usam solicitação; demais usam criação. Situação efetiva calculada com Clock. Limite de leitura: 100.000 registros candidatos. Sem chave técnica do comprovante. Arquivo completo ou 413.") @ExportResponse
    public ResponseEntity<byte[]> payments(@ParameterObject @ModelAttribute BillingExportFilter filters,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(Product.PAYMENTS,filters,format));
    }
    @GetMapping(value="/billing/admin/refunds/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('BILLING_EXPORT')") @RequiredPermission("BILLING_EXPORT")
    @Operation(summary="Exportar reembolsos",description="Todos, por ID ou filtros combinados. Ano/mês são derivados do vencimento do ciclo, e eventId do contexto estruturado. Pagamentos usam período de submissão; reembolsos usam solicitação; demais usam criação. Situação efetiva calculada com Clock. Limite de leitura: 100.000 registros candidatos. Sem chave técnica do comprovante. Arquivo completo ou 413.") @ExportResponse
    public ResponseEntity<byte[]> refunds(@ParameterObject @ModelAttribute BillingExportFilter filters,@RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(Product.REFUNDS,filters,format));
    }
}
