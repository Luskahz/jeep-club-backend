package com.jeepclub.backend.authorization.api.controller;


import com.jeepclub.backend.authorization.api.dto.ReplaceUserRolesRequestDTO;
import com.jeepclub.backend.authorization.api.dto.RoleResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/users")
@RequiredArgsConstructor
@Validated
public class UserRoleController{
    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<RoleResponseDTO>> findRolesByUser(
            @PathVariable @Positive Long userId
    ) {
        return null;
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<Void> replaceUserRoles(
            @PathVariable @Positive Long userId,
            @RequestBody @Valid ReplaceUserRolesRequestDTO request
    ) {
        return null;
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long roleId
    ) {
        return null;
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRoleFromUser(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long roleId
    ) {
        return null;
    }
}