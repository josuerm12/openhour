package com.openhour.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityUpdateRequest(@NotNull LocalDate date, @NotBlank String time, boolean open) {
}
