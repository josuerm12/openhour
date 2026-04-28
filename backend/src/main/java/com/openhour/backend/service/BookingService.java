package com.openhour.backend.service;

import com.openhour.backend.model.Booking;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {
    private final List<Booking> bookings = new ArrayList<>();
    private static final long CANCELLATION_WINDOW_HOURS = 24;

    public Booking createBooking(String customerName, String serviceType, LocalDateTime appointmentDate) {
        Booking booking = new Booking(customerName, serviceType, appointmentDate);
        bookings.add(booking);
        return booking;
    }

    public Optional<Booking> getBooking(String id) {
        return bookings.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst();
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }

    public Optional<String> cancelBooking(String id) {
        Optional<Booking> booking = getBooking(id);

        if (booking.isEmpty()) {
            return Optional.of("Booking not found");
        }

        Booking b = booking.get();

        if (b.getStatus() == Booking.BookingStatus.CANCELLED) {
            return Optional.of("Booking is already cancelled");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = b.getAppointmentDate();
        long hoursDifference = java.time.temporal.ChronoUnit.HOURS.between(now, appointmentTime);

        if (hoursDifference < CANCELLATION_WINDOW_HOURS) {
            return Optional.of("Cannot cancel within 24 hours of the appointment");
        }

        b.setStatus(Booking.BookingStatus.CANCELLED);
        return Optional.empty(); // Empty = success
    }

    public void deleteBooking(String id) {
        bookings.removeIf(b -> b.getId().equals(id));
    }
}
