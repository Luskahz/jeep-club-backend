package com.jeepclub.backend.vehicles.api.controller;


import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.dto.include.IncludeResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles - Include",
        description = "Inclusão de veículos de um membro."
)
public class IncludeController {

    @PostMapping
    public void includeVehicle(
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();


    }


}
