package com.jeepclub.backend.iam.identity.api.http.controller;

import com.jeepclub.backend.iam.identity.api.http.dto.user.BirthdayItemDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.BirthdayResponseDTO;
import com.jeepclub.backend.iam.identity.core.application.service.user.BirthdayQueryService;
import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequiresMembership
public class BirthdayController {
    private final BirthdayQueryService birthdays;

    @GetMapping("/identity/users/birthdays")
    @Operation(summary = "Consultar aniversariantes do mês", description = "Inclui usuários ativos de Identity, mesmo sem associação.", responses = {
            @ApiResponse(responseCode = "200", description = "Aniversariantes", headers = @Header(name = "Cache-Control", description = "no-store", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Mês inválido"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "402", description = "Pagamento exigível"),
            @ApiResponse(responseCode = "503", description = "Consulta financeira indisponível")})
    public ResponseEntity<BirthdayResponseDTO> birthdays(@RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new BirthdayResponseDTO(month, birthdays.findByMonth(month).stream().map(BirthdayItemDTO::from).toList()));
    }
}
