package com.jeepclub.backend.authentication.api.dto.logout;

import jakarta.validation.constraints.NotBlank;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;

public record LogoutRequestDTO(
        @NotBlank Authentication refreshToken
) {}