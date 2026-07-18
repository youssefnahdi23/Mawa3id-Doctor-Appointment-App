package com.mawa3id.donation;

import com.fasterxml.jackson.databind.JsonNode;
import com.mawa3id.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Production {@link KonnectGateway} backed by the Konnect API
 * (<a href="https://docs.konnect.network">docs.konnect.network</a>, v2).
 *
 * <p>Flow: {@code POST /payments/init-payment} (authenticated with the {@code x-api-key}
 * header) returns {@code payUrl} + {@code paymentRef}; Konnect later calls the configured
 * webhook with {@code ?payment_ref=}, and the server confirms via
 * {@code GET /payments/{paymentRef}}. The API root is configurable
 * ({@code KONNECT_BASE_URL}) so the sandbox can be used while testing.
 *
 * <p>Excluded from the test profile: it makes real network calls. Domain logic is covered
 * through {@code StubKonnectGateway}.
 */
@Component
@Profile("!test")
public class KonnectPaymentGateway implements KonnectGateway {

    private static final String DESCRIPTION = "Donation to Mawa3id";
    /** Offer the aggregator's Tunisian methods: wallet (Flouci et al.), cards, e-DINAR (D17). */
    private static final List<String> ACCEPTED_METHODS = List.of("wallet", "bank_card", "e-DINAR");
    /** Checkout lifespan in minutes before Konnect expires the session. */
    private static final int LIFESPAN_MINUTES = 30;

    private final DonationProperties.Konnect config;
    private final RestClient restClient;

    public KonnectPaymentGateway(DonationProperties properties) {
        this(properties, RestClient.builder().baseUrl(properties.getKonnect().getBaseUrl()).build());
    }

    KonnectPaymentGateway(DonationProperties properties, RestClient restClient) {
        this.config = properties.getKonnect();
        this.restClient = restClient;
    }

    @Override
    public PaymentGateway.CheckoutSession createCheckoutSession(Donation donation) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("receiverWalletId", config.getWalletId());
        body.put("token", donation.getCurrency().toUpperCase());
        body.put("amount", donation.getAmountMinor());
        body.put("type", "immediate");
        body.put("description", DESCRIPTION);
        body.put("acceptedPaymentMethods", ACCEPTED_METHODS);
        body.put("lifespan", LIFESPAN_MINUTES);
        body.put("orderId", String.valueOf(donation.getId()));
        if (!config.getWebhookUrl().isBlank()) {
            body.put("webhook", config.getWebhookUrl());
            body.put("silentWebhook", true);
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/payments/init-payment")
                    .header("x-api-key", config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.hasNonNull("payUrl") || !response.hasNonNull("paymentRef")) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Unexpected response from the payment provider");
            }
            return new PaymentGateway.CheckoutSession(
                    response.get("paymentRef").asText(), response.get("payUrl").asText());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Could not create a payment session: " + e.getMessage());
        }
    }

    @Override
    public PaymentGateway.WebhookEvent.Type fetchPaymentStatus(String paymentRef) {
        try {
            JsonNode response = restClient.get()
                    .uri("/payments/{ref}", paymentRef)
                    .header("x-api-key", config.getApiKey())
                    .retrieve()
                    .body(JsonNode.class);
            String status = response == null ? ""
                    : response.path("payment").path("status").asText("");
            return switch (status) {
                case "completed" -> PaymentGateway.WebhookEvent.Type.COMPLETED;
                case "failed", "failed_payment" -> PaymentGateway.WebhookEvent.Type.FAILED;
                case "expired" -> PaymentGateway.WebhookEvent.Type.EXPIRED;
                default -> PaymentGateway.WebhookEvent.Type.IGNORED;
            };
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Could not verify the payment status: " + e.getMessage());
        }
    }
}
