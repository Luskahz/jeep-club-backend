package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.ReplaceUserRolesRequestDTO;
import com.jeepclub.backend.authorization.api.dto.RoleResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.application.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/users")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - User Roles",
        description = "Gerenciamento de roles vinculadas a usuários."
)
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping("/{userId}/roles")
    @Operation(
            summary = "Listar roles de um usuário",
            description = "Retorna todas as roles vinculadas a um usuário."
    )
    public ResponseEntity<List<RoleResponseDTO>> findRolesByUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId
    ) {
        RolesResult result = userRoleService.findRolesByUserId(userId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @PutMapping("/{userId}/roles")
    @Operation(
            summary = "Substituir roles de um usuário",
            description = "Substitui completamente o conjunto de roles vinculadas ao usuário. Uma lista vazia remove todas as roles."
    )
    public ResponseEntity<Void> replaceUserRoles(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @RequestBody @Valid ReplaceUserRolesRequestDTO request
    ) {
        userRoleService.replaceUserRoles(
                userId,
                request.roleIds()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(
            summary = "Atribuir role a um usuário",
            description = "Cria o vínculo entre um usuário e uma role ativa."
    )
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @Parameter(
                    description = "ID da role.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        userRoleService.assignRoleToUser(userId, roleId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(
            summary = "Remover role de um usuário",
            description = "Remove o vínculo entre um usuário e uma role."
    )
    public ResponseEntity<Void> revokeRoleFromUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @Parameter(
                    description = "ID da role.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        userRoleService.revokeRoleFromUser(userId, roleId);

        return ResponseEntity.noContent().build();
    }
}