package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.application.service.RolePermissionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/roles/{roleId}/permissions")
@RequiredArgsConstructor
@Validated
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    public ResponseEntity<List<PermissionResponseDTO>> findPermissionsByRoleId(
            @PathVariable @Positive Long roleId
    ) {
        PermissionsResult result = rolePermissionService.findPermissionsByRoleId(roleId);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permissions())
        );
    }

    @PostMapping("/{permissionId}")
    public ResponseEntity<Void> assignPermissionToRole(
            @PathVariable @Positive Long roleId,
            @PathVariable @Positive Long permissionId
    ) {
        rolePermissionService.assignPermissionToRole(roleId, permissionId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable @Positive Long roleId,
            @PathVariable @Positive Long permissionId
    ) {
        rolePermissionService.removePermissionFromRole(roleId, permissionId);

        return ResponseEntity.noContent().build();
    }
}