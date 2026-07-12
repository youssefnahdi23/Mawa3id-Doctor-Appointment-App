package com.mawa3id.donation;

/**
 * Abstraction over a card payment provider. The production implementation talks to
 * Stripe; tests supply a deterministic stub so the donation flow — including webhook
 * handling — can be exercised without network access or real keys.
 */
public interface PaymentGateway {

    /**
     * Create a hosted checkout session for the given (already-persisted) donation.
     *
     * @return the provider session id and the URL the donor should be redirected to.
     */
    CheckoutSession createCheckoutSession(Donation donation);

    /**
     * Verify the authenticity of an incoming webhook payload and parse it into a
     * provider-neutral event.
     *
     * @param payload         the raw request body, exactly as received
     * @param signatureHeader the provider signature header
     * @throws com.mawa3id.common.ApiException if the signature is invalid or the payload
     *                                         cannot be parsed
     */
    WebhookEvent verifyAndParse(String payload, String signatureHeader);

    /** A created checkout session: the provider session id and the redirect URL. */
    record CheckoutSession(String sessionId, String checkoutUrl) {
    }

    /** A provider-neutral webhook event relevant to the donation lifecycle. */
    record WebhookEvent(Type type, String sessionId, String paymentRef) {
        public enum Type {
            /** Payment completed — donation should be marked succeeded. */
            COMPLETED,
            /** The checkout session expired without payment. */
            EXPIRED,
            /** Payment failed. */
            FAILED,
            /** An event we do not act on. */
            IGNORED
        }
    }
}
