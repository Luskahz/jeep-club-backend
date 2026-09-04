package com.jeepclub.backend.identity.api.http.controller;

import com.jeepclub.backend.identity.api.http.dto.user.CurrentUserResponseDTO;
import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/identity", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Identity - Users", description = "Cadastro e consulta dos dados cadastrais dos usuários.")
public class CurrentUserController {
    private final UserQuery userQuery;

    @GetMapping("/me")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(summary = "Consultar meus dados cadastrais")
    public ResponseEntity<CurrentUserResponseDTO> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserDetails user = userQuery.findById(principal.getUserId())
                .orElseThrow(() -> new UserNotFoundException(principal.getUserId()));
        return ResponseEntity.ok(CurrentUserResponseDTO.from(user));
    }
}
