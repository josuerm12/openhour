package com.openhour.backend.controller;

import com.openhour.backend.dto.AvailabilitySlotResponse;
import com.openhour.backend.dto.AvailabilityUpdateRequest;
import com.openhour.backend.service.AdminAuthService;
import com.openhour.backend.service.AvailabilityService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {
    private final AvailabilityService availabilityService;
    private final AdminAuthService adminAuthService;

    public AvailabilityController(AvailabilityService availabilityService, AdminAuthService adminAuthService) {
        this.availabilityService = availabilityService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public List<AvailabilitySlotResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return availabilityService.list(start, end);
    }

    @GetMapping("/day")
    public List<AvailabilitySlotResponse> listForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return availabilityService.listForDate(date);
    }

    @PutMapping
    public AvailabilitySlotResponse update(
            @Valid @RequestBody AvailabilityUpdateRequest request,
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken
    ) {
        adminAuthService.requireAdmin(adminToken);
        return availabilityService.setOpen(request.date(), request.time(), request.open());
    }
}
