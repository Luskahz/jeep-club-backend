package com.jeepclub.backend.vehicles.api.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles - Delete",
        description = "Deleção de veículos de um membro."
)
public class DeleteController {


}
