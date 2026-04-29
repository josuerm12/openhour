package com.openhour.backend.dto;

public record CheckoutResponse(Long appointmentId, String checkoutUrl, String stripeSessionId) {
}
