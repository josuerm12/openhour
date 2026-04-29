package com.openhour.backend.controller;

import com.openhour.backend.dto.AppointmentRequest;
import com.openhour.backend.dto.AppointmentResponse;
import com.openhour.backend.dto.CheckoutResponse;
import com.openhour.backend.dto.MoveAppointmentRequest;
import com.openhour.backend.service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentResponse> list() {
        return appointmentService.listConfirmed();
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse get(@PathVariable Long appointmentId) {
        return appointmentService.get(appointmentId);
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.createCheckout(request);
    }

    @PostMapping("/{appointmentId}/confirm")
    public AppointmentResponse confirm(
            @PathVariable Long appointmentId,
            @RequestParam("session_id") String sessionId
    ) {
        return appointmentService.confirm(appointmentId, sessionId);
    }

    @PatchMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@PathVariable Long appointmentId) {
        return appointmentService.cancel(appointmentId);
    }

    @PatchMapping("/{appointmentId}/move")
    public AppointmentResponse move(
            @PathVariable Long appointmentId,
            @Valid @RequestBody MoveAppointmentRequest request
    ) {
        return appointmentService.move(appointmentId, request);
    }
}
