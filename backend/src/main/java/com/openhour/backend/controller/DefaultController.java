package com.openhour.backend.controller;

import com.openhour.backend.dto.BookingDTO;
import com.openhour.backend.dto.CancellationRequest;
import com.openhour.backend.dto.CancellationResponse;
import com.openhour.backend.model.Booking;
import com.openhour.backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController 
public class DefaultController {
    @Autowired
    private BookingService bookingService;

    @GetMapping("/")
    public String home() {
        return "Welcome to the OpenHour API!";
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingDTO bookingDTO) {
        Booking booking = bookingService.createBooking(
                bookingDTO.getCustomerName(),
                bookingDTO.getServiceType(),
                bookingDTO.getAppointmentDate()
        );
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/api/bookings")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        List<BookingDTO> bookings = bookingService.getAllBookings()
                .stream()
                .map(b -> new BookingDTO(b.getCustomerName(), b.getServiceType(), b.getAppointmentDate()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<Booking> getBooking(@PathVariable String id) {
        Optional<Booking> booking = bookingService.getBooking(id);
        return booking.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<CancellationResponse> cancelBooking(@PathVariable String id) {
        Optional<String> error = bookingService.cancelBooking(id);

        if (error.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new CancellationResponse(false, error.get()));
        }

        return ResponseEntity.ok(new CancellationResponse(true, "Appointment cancelled successfully"));
    }
}
