package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.core.application.services.ListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles/list")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vehicles", description = "")
public class ListController {

    private final ListService listService;

    @GetMapping("/member")
    @Operation(
            summary = "Listar veículos do membro logado",
            description = "Retorna os veículos ativos do membro autenticado"
    )
    public ResponseEntity<Page<ListResponseDTO>> listMemberVehicles(
            Authentication authentication,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long memberId = principal.getUserId();

        return ResponseEntity.ok(listService.execute(memberId, pageable));
    }

    @GetMapping("/admin")
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar todos os veículos",
            description = "Retorna todos os veículos ativos da plataforma"
    )
    public ResponseEntity<Page<ListResponseDTO>> listAllVehicles(
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(listService.executeAsAdmin(pageable));
    }
}
