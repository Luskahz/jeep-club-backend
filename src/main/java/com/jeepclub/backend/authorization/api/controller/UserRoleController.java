package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.ReplaceUserRolesRequestDTO;
import com.jeepclub.backend.authorization.api.dto.RoleResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.FindAllRolesResult;
import com.jeepclub.backend.authorization.core.application.result.userrole.FindUserRolesResult;
import com.jeepclub.backend.authorization.core.application.service.UserRoleService;
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
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<RoleResponseDTO>> findRolesByUser(
            @PathVariable @Positive Long userId
    ) {
        FindAllRolesResult result = userRoleService.findRolesByUserId(userId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<Void> replaceUserRoles(
            @PathVariable @Positive Long userId,
            @RequestBody @Valid ReplaceUserRolesRequestDTO request
    ) {
        userRoleService.replaceUserRoles(
                userId,
                request.roleIds()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long roleId
    ) {
        userRoleService.assignRoleToUser(userId, roleId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRoleFromUser(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long roleId
    ) {
        userRoleService.revokeRoleFromUser(userId, roleId);

        return ResponseEntity.noContent().build();
    }
}