package com.mawa3id.donation;

import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.donation.dto.CreateDonationRequest;
import com.mawa3id.donation.dto.DonationConfigResponse;
import com.mawa3id.donation.dto.DonationResponse;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DonationService {

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final KonnectGateway konnectGateway;
    private final DonationProperties properties;

    public DonationService(DonationRepository donationRepository, UserRepository userRepository,
                           PaymentGateway paymentGateway, KonnectGateway konnectGateway,
                           DonationProperties properties) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.konnectGateway = konnectGateway;
        this.properties = properties;
    }

    /**
     * Start a donation on the requested channel: persist a PENDING record and, for hosted
     * providers (Stripe / Konnect), open a checkout session and return the redirect URL.
     * RIB donations record the donor's declared bank transfer for manual reconciliation
     * (no checkout URL).
     *
     * @param userId the authenticated donor, or {@code null} for an anonymous donation
     */
    @Transactional
    public DonationResponse create(Long userId, CreateDonationRequest request) {
        DonationProvider provider = request.providerOrDefault();
        requireAvailable(provider);
        if (request.amountMinor() < properties.getMinAmountMinor()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Minimum donation is " + properties.getMinAmountMinor() + " (minor units)");
        }

        String currency = currencyFor(provider, request.currency());
        User user = userId == null ? null : userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Donation donation = donationRepository.save(new Donation(user, request.amountMinor(), currency,
                provider, request.donorName(), request.message()));

        return switch (provider) {
            case STRIPE -> {
                PaymentGateway.CheckoutSession session = paymentGateway.createCheckoutSession(donation);
                donation.attachSession(session.sessionId());
                yield DonationResponse.from(donation, session.checkoutUrl());
            }
            case KONNECT -> {
                PaymentGateway.CheckoutSession session = konnectGateway.createCheckoutSession(donation);
                donation.attachSession(session.sessionId());
                yield DonationResponse.from(donation, session.checkoutUrl());
            }
            // Manual transfer: stays PENDING until reconciled by hand against the bank account.
            case RIB -> DonationResponse.from(donation);
        };
    }

    private void requireAvailable(DonationProvider provider) {
        boolean available = properties.isEnabled() && switch (provider) {
            case STRIPE -> true;
            case KONNECT -> properties.getKonnect().isConfigured();
            case RIB -> properties.getRib().isConfigured();
        };
        if (!available) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "This donation method is currently unavailable");
        }
    }

    /**
     * Konnect and RIB amounts are always in their configured (Tunisian) currency; only
     * Stripe honours a caller-supplied currency, defaulting to the global one.
     */
    private String currencyFor(DonationProvider provider, String requested) {
        return switch (provider) {
            case STRIPE -> (requested != null && !requested.isBlank())
                    ? requested.toLowerCase()
                    : properties.getCurrency();
            case KONNECT -> properties.getKonnect().getCurrency();
            case RIB -> properties.getRib().getCurrency();
        };
    }

    /**
     * Apply a provider webhook to the matching donation. Idempotent: unknown sessions and
     * already-finalised donations are ignored so the provider always receives an ack.
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        PaymentGateway.WebhookEvent event = paymentGateway.verifyAndParse(payload, signatureHeader);
        if (event.type() == PaymentGateway.WebhookEvent.Type.IGNORED || event.sessionId() == null) {
            return;
        }
        Optional<Donation> found = donationRepository.findByProviderSessionId(event.sessionId());
        if (found.isEmpty()) {
            return;
        }
        Donation donation = found.get();
        if (donation.getStatus() != DonationStatus.PENDING) {
            return;
        }
        switch (event.type()) {
            case COMPLETED -> donation.markSucceeded(event.paymentRef());
            case EXPIRED -> donation.markExpired();
            case FAILED -> donation.markFailed();
            default -> {
                // IGNORED handled above; no other cases act on a donation.
            }
        }
    }

    /**
     * Apply a Konnect callback. Konnect does not sign payloads; it passes a
     * {@code payment_ref} and the server fetches the authoritative status from the
     * Konnect API. Idempotent: unknown refs and already-finalised donations are ignored.
     */
    @Transactional
    public void handleKonnectWebhook(String paymentRef) {
        if (paymentRef == null || paymentRef.isBlank()) {
            return;
        }
        Optional<Donation> found = donationRepository.findByProviderSessionId(paymentRef);
        if (found.isEmpty()) {
            return;
        }
        Donation donation = found.get();
        if (donation.getProvider() != DonationProvider.KONNECT
                || donation.getStatus() != DonationStatus.PENDING) {
            return;
        }
        switch (konnectGateway.fetchPaymentStatus(paymentRef)) {
            case COMPLETED -> donation.markSucceeded(paymentRef);
            case EXPIRED -> donation.markExpired();
            case FAILED -> donation.markFailed();
            case IGNORED -> {
                // Still pending on Konnect's side; a later callback will settle it.
            }
        }
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> listForUser(Long userId) {
        return donationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DonationResponse::from)
                .toList();
    }

    public DonationConfigResponse config() {
        boolean enabled = properties.isEnabled();
        DonationProperties.Konnect konnect = properties.getKonnect();
        DonationProperties.Rib rib = properties.getRib();
        boolean ribAvailable = enabled && rib.isConfigured();
        return new DonationConfigResponse(
                enabled,
                properties.getCurrency(),
                properties.getMinAmountMinor(),
                properties.getPatreonUrl(),
                enabled && konnect.isConfigured(),
                konnect.getCurrency(),
                ribAvailable ? rib.getAccountHolder() : "",
                ribAvailable ? rib.getBankName() : "",
                ribAvailable ? rib.getNumber() : "",
                rib.getCurrency());
    }
}
