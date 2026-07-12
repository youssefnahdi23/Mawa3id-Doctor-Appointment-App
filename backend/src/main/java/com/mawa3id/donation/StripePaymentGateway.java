package com.mawa3id.donation;

import com.mawa3id.common.ApiException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Production {@link PaymentGateway} backed by Stripe Checkout.
 *
 * <p>Excluded from the test profile (and from the JaCoCo coverage gate) because it makes
 * real network calls to Stripe and verifies real webhook signatures, neither of which can
 * run in CI without live keys. The donation domain logic that <em>uses</em> this gateway
 * is fully covered via {@code StubPaymentGateway} in tests.
 */
@Component
@Profile("!test")
public class StripePaymentGateway implements PaymentGateway {

    private static final String PRODUCT_NAME = "Donation to Mawa3id";

    private final DonationProperties.Stripe config;

    public StripePaymentGateway(DonationProperties properties) {
        this.config = properties.getStripe();
        Stripe.apiKey = config.getSecretKey();
    }

    @Override
    public CheckoutSession createCheckoutSession(Donation donation) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(config.getSuccessUrl())
                .setCancelUrl(config.getCancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(donation.getCurrency())
                                .setUnitAmount(donation.getAmountMinor())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(PRODUCT_NAME)
                                        .build())
                                .build())
                        .build())
                .putMetadata("donationId", String.valueOf(donation.getId()))
                .build();
        try {
            Session session = Session.create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Could not create a payment session: " + e.getMessage());
        }
    }

    @Override
    public WebhookEvent verifyAndParse(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, config.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        String sessionId = session != null ? session.getId() : null;
        String paymentRef = session != null ? session.getPaymentIntent() : null;

        WebhookEvent.Type type = switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    WebhookEvent.Type.COMPLETED;
            case "checkout.session.expired" -> WebhookEvent.Type.EXPIRED;
            case "checkout.session.async_payment_failed" -> WebhookEvent.Type.FAILED;
            default -> WebhookEvent.Type.IGNORED;
        };
        return new WebhookEvent(type, sessionId, paymentRef);
    }
}
