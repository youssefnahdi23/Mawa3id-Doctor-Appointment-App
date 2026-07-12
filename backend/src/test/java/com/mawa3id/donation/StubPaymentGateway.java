package com.mawa3id.donation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawa3id.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Deterministic {@link PaymentGateway} used under the {@code test} profile so the whole
 * donation flow — creation and webhook handling — runs in CI with no network or keys.
 *
 * <p>Checkout sessions get a stable id derived from the donation id
 * ({@code cs_test_<id>}); tests can therefore reconstruct the id to drive a webhook.
 * {@link #verifyAndParse} reads a simplified JSON envelope
 * ({@code {"type": "...", "sessionId": "...", "paymentRef": "..."}}) instead of verifying
 * a real Stripe signature.
 */
@Component
@Profile("test")
public class StubPaymentGateway implements PaymentGateway {

    static final String SESSION_PREFIX = "cs_test_";
    static final String URL_PREFIX = "https://stripe.test/checkout/";

    private final ObjectMapper objectMapper;

    public StubPaymentGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CheckoutSession createCheckoutSession(Donation donation) {
        String sessionId = SESSION_PREFIX + donation.getId();
        return new CheckoutSession(sessionId, URL_PREFIX + sessionId);
    }

    @Override
    public WebhookEvent verifyAndParse(String payload, String signatureHeader) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            WebhookEvent.Type type = mapType(node.path("type").asText(""));
            String sessionId = node.hasNonNull("sessionId") ? node.get("sessionId").asText() : null;
            String paymentRef = node.hasNonNull("paymentRef") ? node.get("paymentRef").asText() : null;
            return new WebhookEvent(type, sessionId, paymentRef);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
        }
    }

    private WebhookEvent.Type mapType(String raw) {
        return switch (raw) {
            case "checkout.session.completed" -> WebhookEvent.Type.COMPLETED;
            case "checkout.session.expired" -> WebhookEvent.Type.EXPIRED;
            case "checkout.session.async_payment_failed" -> WebhookEvent.Type.FAILED;
            default -> WebhookEvent.Type.IGNORED;
        };
    }
}
