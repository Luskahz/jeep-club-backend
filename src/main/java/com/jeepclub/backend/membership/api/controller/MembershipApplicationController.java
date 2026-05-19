package com.jeepclub.backend.membership.api.controller;



import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/membership-applications")
public class MembershipApplicationController {

    private final CreateMembershipApplicationService createMembershipApplicationService;

    public MembershipApplicationController(
            CreateMembershipApplicationService createMembershipApplicationService
    ) {
        this.createMembershipApplicationService = createMembershipApplicationService;
    }

    @PostMapping
    public ResponseEntity<MembershipApplicationResponseDTO> create(
            @RequestBody @Valid CreateMembershipApplicationRequestDTO request
    ) {
        MembershipApplication application = createMembershipApplicationService.create(
                request.name(),
                request.cpf(),
                request.email(),
                request.phoneNumber(),
                request.message()
        );

        MembershipApplicationResponseDTO response = MembershipApplicationResponseDTO.fromDomain(application);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}