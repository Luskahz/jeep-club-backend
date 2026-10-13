package com.jeepclub.backend.iam.authentication.api.http.dto.activation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAccountActivationDTO(@NotBlank @Size(max = 256) String token,
                                           @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
