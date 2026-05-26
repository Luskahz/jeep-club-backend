package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.dto.detail.DetailResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles - Detail",
        description = "Visualização de um veículo de um membro."
)
public class DetailController {

    @GetMapping("/{vehicleId}")
    @Operation(
            summary = "Detalhar 1 veículo de um membro",
            description = "Retorna os detalhes de um veículo pelo seu ID"
    )
    public ResponseEntity<DetailResponseDTO> detail(
            @PathVariable Long vehicleId,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();



        return ResponseEntity.ok().build();
    }
}