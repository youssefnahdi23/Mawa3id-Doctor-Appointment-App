package com.mawa3id.donation;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic {@link KonnectGateway} used under the {@code test} profile.
 *
 * <p>Checkouts get a stable reference derived from the donation id
 * ({@code konnect_test_<id>}), so tests can reconstruct it to drive the webhook.
 * {@link #fetchPaymentStatus} is keyed off the reference content: refs containing
 * {@code fail} report FAILED, {@code expired} report EXPIRED, anything else COMPLETED.
 */
@Component
@Profile("test")
public class StubKonnectGateway implements KonnectGateway {

    static final String REF_PREFIX = "konnect_test_";
    static final String URL_PREFIX = "https://konnect.test/pay/";

    @Override
    public PaymentGateway.CheckoutSession createCheckoutSession(Donation donation) {
        String ref = REF_PREFIX + donation.getId();
        return new PaymentGateway.CheckoutSession(ref, URL_PREFIX + ref);
    }

    @Override
    public PaymentGateway.WebhookEvent.Type fetchPaymentStatus(String paymentRef) {
        if (paymentRef.contains("fail")) {
            return PaymentGateway.WebhookEvent.Type.FAILED;
        }
        if (paymentRef.contains("expired")) {
            return PaymentGateway.WebhookEvent.Type.EXPIRED;
        }
        return PaymentGateway.WebhookEvent.Type.COMPLETED;
    }
}
