package com.jeepclub.backend.iam.authentication.api.http.dto.activation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivationInvitationTokenDTO(@NotBlank @Size(max = 256) String token) {
}
