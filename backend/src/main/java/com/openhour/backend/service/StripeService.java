package com.openhour.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhour.backend.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class StripeService {
    private final String secretKey;
    private final String frontendUrl;
    private final RestClient restClient;

    public StripeService(
            @Value("${app.stripe.secret-key}") String secretKey,
            @Value("${app.frontend-url}") String frontendUrl,
            RestClient.Builder restClientBuilder
    ) {
        this.secretKey = secretKey;
        this.frontendUrl = frontendUrl;
        this.restClient = restClientBuilder.baseUrl("https://api.stripe.com").build();
    }

    public StripeCheckoutSession createCheckoutSession(
            Long appointmentId,
            String customerEmail,
            String serviceName,
            int depositCents,
            int donationCents
    ) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("submit_type", "book");
        form.add("customer_email", customerEmail);
        form.add("success_url", frontendUrl + "/frontend/index.html?payment=success&appointmentId=" + appointmentId + "&session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", frontendUrl + "/frontend/index.html?payment=cancelled&appointmentId=" + appointmentId);
        form.add("metadata[appointmentId]", String.valueOf(appointmentId));

        List<LineItem> items = new ArrayList<>();
        items.add(new LineItem("Deposit for " + serviceName, depositCents));
        if (donationCents > 0) {
            items.add(new LineItem("Optional donation/tip", donationCents));
        }

        for (int index = 0; index < items.size(); index += 1) {
            LineItem item = items.get(index);
            form.add("line_items[" + index + "][price_data][currency]", "usd");
            form.add("line_items[" + index + "][price_data][product_data][name]", item.name());
            form.add("line_items[" + index + "][price_data][unit_amount]", String.valueOf(item.amountCents()));
            form.add("line_items[" + index + "][quantity]", "1");
        }

        JsonNode response = restClient.post()
                .uri("/v1/checkout/sessions")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("id") || !response.hasNonNull("url")) {
            throw new BadRequestException("Stripe did not return a checkout URL.");
        }
        return new StripeCheckoutSession(response.get("id").asText(), response.get("url").asText(), null);
    }

    public StripeCheckoutSession createDonationSession(int amountCents) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("submit_type", "donate");
        form.add("success_url", frontendUrl + "/frontend/index.html?payment=donation_success&session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", frontendUrl + "/frontend/index.html?payment=donation_cancelled");
        form.add("line_items[0][price_data][currency]", "usd");
        form.add("line_items[0][price_data][product_data][name]", "OpenHour donation/tip");
        form.add("line_items[0][price_data][unit_amount]", String.valueOf(amountCents));
        form.add("line_items[0][quantity]", "1");

        JsonNode response = restClient.post()
                .uri("/v1/checkout/sessions")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("id") || !response.hasNonNull("url")) {
            throw new BadRequestException("Stripe did not return a donation checkout URL.");
        }
        return new StripeCheckoutSession(response.get("id").asText(), response.get("url").asText(), null);
    }

    public StripeCheckoutSession retrieveCheckoutSession(String sessionId) {
        requireConfigured();
        JsonNode response = restClient.get()
                .uri("/v1/checkout/sessions/{sessionId}", sessionId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.hasNonNull("id")) {
            throw new BadRequestException("Stripe checkout session was not found.");
        }
        String paymentStatus = response.path("payment_status").asText();
        String paymentIntent = response.path("payment_intent").isMissingNode() ? null : response.path("payment_intent").asText(null);
        return new StripeCheckoutSession(response.get("id").asText(), response.path("url").asText(null), paymentStatus, paymentIntent);
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BadRequestException("Stripe is not configured. Set STRIPE_SECRET_KEY before creating checkout sessions.");
        }
    }

    private String authorizationHeader() {
        String token = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private record LineItem(String name, int amountCents) {
    }

    public record StripeCheckoutSession(String id, String url, String paymentStatus, String paymentIntentId) {
        public StripeCheckoutSession(String id, String url, String paymentStatus) {
            this(id, url, paymentStatus, null);
        }
    }
}
