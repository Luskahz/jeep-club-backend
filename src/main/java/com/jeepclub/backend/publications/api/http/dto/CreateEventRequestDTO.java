package com.jeepclub.backend.publications.api.http.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
@Schema(description="Creates an OPEN event and DRAFT publication. Author is the authenticated user. Charge definitions may be selected or created inline atomically.")
public record CreateEventRequestDTO(@NotBlank @Size(max=200) String title, @NotBlank String content,
        @NotNull @Size(min=1,max=5) List<@Valid PublicationImageRequestDTO> images,
        @NotNull Instant startsAt, Instant endsAt, List<EventRequestDTO.Charge> charges) {}
