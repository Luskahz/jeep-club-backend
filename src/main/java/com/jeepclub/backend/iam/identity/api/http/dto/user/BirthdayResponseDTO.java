package com.jeepclub.backend.iam.identity.api.http.dto.user;

import java.util.List;

public record BirthdayResponseDTO(int month, List<BirthdayItemDTO> birthdays) {}
