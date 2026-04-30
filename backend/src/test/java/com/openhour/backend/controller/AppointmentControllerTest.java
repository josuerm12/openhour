package com.openhour.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openhour.backend.dto.AppointmentRequest;
import com.openhour.backend.dto.CheckoutResponse;
import com.openhour.backend.exception.ApiExceptionHandler;
import com.openhour.backend.service.AdminAuthService;
import com.openhour.backend.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AppointmentControllerTest {
    private MockMvc mockMvc;
    private FakeAppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new FakeAppointmentService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                        appointmentService,
                        new AdminAuthService("owner", "secret", "admin-token")
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void checkoutReturnsBadRequestForInvalidBookingRequest() throws Exception {
        String invalidRequest = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "phone": "555-1212",
                  "service": "Silk Press",
                  "date": null,
                  "time": "10:00 AM",
                  "donationCents": 0
                }
                """;

        mockMvc.perform(post("/api/appointments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertThat(appointmentService.createCheckoutCalls).isZero();
    }

    private static class FakeAppointmentService extends AppointmentService {
        private int createCheckoutCalls;

        FakeAppointmentService() {
            super(null, null, null, null, null);
        }

        @Override
        public CheckoutResponse createCheckout(AppointmentRequest request) {
            createCheckoutCalls += 1;
            throw new AssertionError("Invalid requests should fail validation before reaching the service.");
        }
    }
}
