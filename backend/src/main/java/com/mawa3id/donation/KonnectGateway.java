package com.mawa3id.donation;

/**
 * Abstraction over the Konnect payment aggregator (D17, Flouci wallet, Tunisian bank
 * cards). Unlike Stripe, Konnect does not sign webhook payloads: it calls the configured
 * webhook URL with a {@code payment_ref} query parameter, and the server then fetches the
 * authoritative payment status from the Konnect API. Tests supply a deterministic stub.
 */
public interface KonnectGateway {

    /**
     * Create a hosted Konnect checkout for the given (already-persisted) donation.
     *
     * @return the Konnect payment reference (stored as the provider session id) and the
     *         hosted payment URL the donor should be redirected to.
     */
    PaymentGateway.CheckoutSession createCheckoutSession(Donation donation);

    /**
     * Fetch the authoritative status of a payment from the Konnect API, mapped onto the
     * provider-neutral event types ({@code IGNORED} for still-pending/unknown states).
     */
    PaymentGateway.WebhookEvent.Type fetchPaymentStatus(String paymentRef);
}
