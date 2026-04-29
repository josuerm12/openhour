package com.openhour.backend.dto;

import com.openhour.backend.model.Appointment;
import com.openhour.backend.model.AppointmentStatus;
import com.openhour.backend.model.PaymentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id,
        String name,
        String email,
        String phone,
        String service,
        String notes,
        LocalDate date,
        String time,
        int depositCents,
        int donationCents,
        AppointmentStatus status,
        PaymentStatus paymentStatus,
        String stripeSessionId,
        String stripePaymentIntentId,
        Instant createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCustomerName(),
                appointment.getEmail(),
                appointment.getPhone(),
                appointment.getServiceName(),
                appointment.getNotes(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getDepositCents(),
                appointment.getDonationCents(),
                appointment.getStatus(),
                appointment.getPaymentStatus(),
                appointment.getStripeSessionId(),
                appointment.getStripePaymentIntentId(),
                appointment.getCreatedAt()
        );
    }
}
