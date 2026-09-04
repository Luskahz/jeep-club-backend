package com.jeepclub.backend.authorization.api.http.controller;

import com.jeepclub.backend.authorization.api.http.dto.CurrentAuthorizationResponseDTO;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authorization - Current user", description = "Permissões do usuário autenticado.")
public class CurrentAuthorizationController {
    @GetMapping("/me")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Consultar minhas authorities",
            description = "Retorna o userId e as authorities efetivas presentes na autenticação atual.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authorities retornadas.",
                            content = @Content(schema = @Schema(implementation = CurrentAuthorizationResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<CurrentAuthorizationResponseDTO> getMe(
            @AuthenticationPrincipal UserPrincipal principal,
            Authentication authentication
    ) {
        var authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return ResponseEntity.ok(new CurrentAuthorizationResponseDTO(
                principal.getUserId(),
                authorities
        ));
    }
}
