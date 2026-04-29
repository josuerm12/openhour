package com.openhour.backend.dto;

import jakarta.validation.constraints.Min;

public record DonationRequest(@Min(100) int amountCents) {
}
