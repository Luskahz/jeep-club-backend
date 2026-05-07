package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.RoleResponseDTO;
import com.jeepclub.backend.authorization.api.dto.role.CreateRoleRequestDTO;
import com.jeepclub.backend.authorization.api.dto.role.UpdateRoleRequestDTO;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.application.result.RoleResult;
import com.jeepclub.backend.authorization.core.application.service.RoleService;
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
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - Roles",
        description = "Gerenciamento de roles de autorização."
)
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(
            summary = "Criar role",
            description = "Cria uma nova role de autorização."
    )
    public ResponseEntity<RoleResponseDTO> createRole(
            @RequestBody @Valid CreateRoleRequestDTO request
    ) {
        RoleResult result = roleService.createRole(
                request.name(),
                request.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoleResponseDTO.from(result.role()));
    }

    @GetMapping
    @Operation(
            summary = "Listar roles",
            description = "Retorna todas as roles de autorização cadastradas."
    )
    public ResponseEntity<List<RoleResponseDTO>> findAllRoles() {
        RolesResult result = roleService.findAllRoles();

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @GetMapping("/{roleId}")
    @Operation(
            summary = "Buscar role por ID",
            description = "Retorna os dados de uma role a partir do seu identificador."
    )
    public ResponseEntity<RoleResponseDTO> findRoleById(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = roleService.findRoleById(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PutMapping("/{roleId}")
    @Operation(
            summary = "Atualizar role",
            description = "Atualiza nome e descrição de uma role de autorização."
    )
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId,

            @RequestBody @Valid UpdateRoleRequestDTO request
    ) {
        RoleResult result = roleService.updateRole(
                roleId,
                request.name(),
                request.description()
        );

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PatchMapping("/{roleId}/deactivate")
    @Operation(
            summary = "Desativar role",
            description = "Marca uma role ativa como inativa."
    )
    public ResponseEntity<RoleResponseDTO> deactivateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = roleService.deactivateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PatchMapping("/{roleId}/activate")
    @Operation(
            summary = "Ativar role",
            description = "Marca uma role inativa como ativa."
    )
    public ResponseEntity<RoleResponseDTO> activateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = roleService.activateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @DeleteMapping("/{roleId}")
    @Operation(
            summary = "Excluir role",
            description = "Realiza exclusão lógica de uma role de autorização."
    )
    public ResponseEntity<Void> deleteRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        roleService.deleteRole(roleId);

        return ResponseEntity.noContent().build();
    }
}