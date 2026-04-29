package com.openhour.backend.controller;

import com.openhour.backend.dto.CheckoutResponse;
import com.openhour.backend.dto.DonationRequest;
import com.openhour.backend.service.ActivityService;
import com.openhour.backend.service.StripeService;
import com.openhour.backend.service.StripeService.StripeCheckoutSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donations")
public class DonationController {
    private final StripeService stripeService;
    private final ActivityService activityService;

    public DonationController(StripeService stripeService, ActivityService activityService) {
        this.stripeService = stripeService;
        this.activityService = activityService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody DonationRequest request) {
        StripeCheckoutSession session = stripeService.createDonationSession(request.amountCents());
        activityService.record("Started donation checkout for $" + (request.amountCents() / 100) + ".");
        return new CheckoutResponse(null, session.url(), session.id());
    }
}
