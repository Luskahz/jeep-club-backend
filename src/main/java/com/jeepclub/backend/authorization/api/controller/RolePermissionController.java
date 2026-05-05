package com.jeepclub.backend.authorization.api.controller;


import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.api.dto.ReplaceRolePermissionsRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor
@Validated
public class RolePermissionController {

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<List<PermissionResponseDTO>> findPermissionsByRole(
            @PathVariable @Positive Long roleId
    ) {
        return null;
    }

    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<Void> replaceRolePermissions(
            @PathVariable @Positive Long roleId,
            @RequestBody @Valid ReplaceRolePermissionsRequestDTO request
    ) {
        return null;
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> assignPermissionToRole(
            @PathVariable @Positive Long roleId,
            @PathVariable @Positive Long permissionId
    ) {
        return null;
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> revokePermissionFromRole(
            @PathVariable @Positive Long roleId,
            @PathVariable @Positive Long permissionId
    ) {
        return null;
    }
}