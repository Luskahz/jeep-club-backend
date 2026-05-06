package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.role.CreateRoleRequestDTO;
import com.jeepclub.backend.authorization.api.dto.RoleResponseDTO;
import com.jeepclub.backend.authorization.api.dto.role.UpdateRoleRequestDTO;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.application.result.role.RoleResult;
import com.jeepclub.backend.authorization.core.application.service.RoleService;
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
public class RoleController {

    private final RoleService roleService;

    @PostMapping
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
    public ResponseEntity<List<RoleResponseDTO>> findAllRoles() {
        RolesResult result = roleService.findAllRoles();

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponseDTO> findRoleById(
            @PathVariable @Positive Long roleId
    ) {
        RoleResult result = roleService.findRoleById(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @PathVariable @Positive Long roleId,
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
    public ResponseEntity<RoleResponseDTO> deactivateRole(
            @PathVariable @Positive Long roleId
    ) {
        RoleResult result = roleService.deactivateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PatchMapping("/{roleId}/activate")
    public ResponseEntity<RoleResponseDTO> activateRole(
            @PathVariable @Positive Long roleId
    ) {
        RoleResult result = roleService.activateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable @Positive Long roleId
    ) {
        roleService.deleteRole(roleId);

        return ResponseEntity.noContent().build();
    }
}