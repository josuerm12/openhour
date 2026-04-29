package com.openhour.backend.dto;

import java.time.LocalDate;

public record AvailabilitySlotResponse(LocalDate date, String time, boolean open, boolean booked) {
}
