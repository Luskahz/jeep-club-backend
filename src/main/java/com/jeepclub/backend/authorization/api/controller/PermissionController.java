package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.permission.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.permission.FindPermissionResult;
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
        FindAllPermissionsResult result = permissionService.findAllPermissions();

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permissions())
        );
    }

    @GetMapping("/{permissionId}")
    public ResponseEntity<PermissionResponseDTO> findPermissionById(
            @PathVariable @Positive Long permissionId
    ) {
        FindPermissionResult result = permissionService.findPermissionById(permissionId);

        return ResponseEntity.ok(PermissionResponseDTO.from(result.permission()));
    }

    @GetMapping("/code/{permissionCode}")
    public ResponseEntity<PermissionResponseDTO> findPermissionByCode(
            @PathVariable String permissionCode
    ) {
        FindPermissionResult result = permissionService.findPermissionByCode(permissionCode);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permission())
        );
    }
}