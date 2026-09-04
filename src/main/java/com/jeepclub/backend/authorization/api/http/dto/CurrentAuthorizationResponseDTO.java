package com.jeepclub.backend.authorization.api.http.dto;

import java.util.List;

public record CurrentAuthorizationResponseDTO(
        Long userId,
        List<String> authorities
) {
    public CurrentAuthorizationResponseDTO {
        authorities = List.copyOf(authorities);
    }
}
