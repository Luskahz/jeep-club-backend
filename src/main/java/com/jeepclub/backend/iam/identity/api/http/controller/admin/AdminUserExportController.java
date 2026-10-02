package com.jeepclub.backend.iam.identity.api.http.controller.admin;
import com.jeepclub.backend.iam.identity.core.application.service.user.UserExportService;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUserFilterDTO;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.*;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor @Tag(name="Identity - Exportações")
public class AdminUserExportController {
    private final UserExportService service;
    @GetMapping(value="/identity/admin/users/export",produces={"text/csv","application/pdf"})
    @PreAuthorize("hasAuthority('IDENTITY_USER_EXPORT')") @RequiredPermission("IDENTITY_USER_EXPORT")
    @Operation(summary="Exportar usuários",description="Todos, por ID ou filtros cadastrais idênticos à listagem. CSV/PDF contém cadastro, estado, datas e existência de foto. Sem paginação HTTP, sem credenciais ou storage keys. Datas em America/Sao_Paulo.")
    @ExportResponse
    public ResponseEntity<byte[]> export(@Valid @ParameterObject @ModelAttribute AdminUserFilterDTO filter,
            @RequestParam(defaultValue="CSV") ExportFormat format) {
        return ExportDownload.response(service.export(filter.toFilter(),format));
    }
}
