package com.openhour.backend.config;

import com.openhour.backend.service.AvailabilityService;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final AvailabilityService availabilityService;

    public DataInitializer(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        availabilityService.ensureSeeded(today, today.plusDays(60));
    }
}
