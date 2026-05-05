package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.FindPermissionResult;
import com.jeepclub.backend.authorization.core.application.service.PermissionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/permissions")
@RequiredArgsConstructor
@Validated
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionResponseDTO>> findAllPermissions() {
        FindAllPermissionsResult result = permissionService.findAllPermissionsFromCatalog();

        List<PermissionResponseDTO> response = result.permissions()
                .stream()
                .map(PermissionResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{permissionId}")
    public ResponseEntity<PermissionResponseDTO> findPermissionById(
            @PathVariable @Positive Long permissionId
    ) {
        FindPermissionResult result = permissionService.findPersistedPermissionById(permissionId);

        return ResponseEntity.ok(PermissionResponseDTO.from(result.permission()));
    }

    @GetMapping("/code/{permissionCode}")
    public ResponseEntity<PermissionResponseDTO> findPermissionByCode(
            @PathVariable String permissionCode
    ) {
        FindPermissionResult result = permissionService.findPermissionByCodeFromCatalog(permissionCode);

        return ResponseEntity.ok(PermissionResponseDTO.from(result.permission()));
    }
}