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
    private final DonationProperties properties;

    public DonationService(DonationRepository donationRepository, UserRepository userRepository,
                           PaymentGateway paymentGateway, DonationProperties properties) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.properties = properties;
    }

    /**
     * Start a donation: persist a PENDING record, open a checkout session with the
     * provider, and return the redirect URL.
     *
     * @param userId the authenticated donor, or {@code null} for an anonymous donation
     */
    @Transactional
    public DonationResponse create(Long userId, CreateDonationRequest request) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Card donations are currently unavailable");
        }
        if (request.amountMinor() < properties.getMinAmountMinor()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Minimum donation is " + properties.getMinAmountMinor() + " (minor units)");
        }

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency().toLowerCase()
                : properties.getCurrency();
        User user = userId == null ? null : userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Donation donation = donationRepository.save(new Donation(user, request.amountMinor(), currency,
                DonationProvider.STRIPE, request.donorName(), request.message()));

        PaymentGateway.CheckoutSession session = paymentGateway.createCheckoutSession(donation);
        donation.attachSession(session.sessionId());
        return DonationResponse.from(donation, session.checkoutUrl());
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

    @Transactional(readOnly = true)
    public List<DonationResponse> listForUser(Long userId) {
        return donationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DonationResponse::from)
                .toList();
    }

    public DonationConfigResponse config() {
        return new DonationConfigResponse(properties.isEnabled(), properties.getCurrency(),
                properties.getMinAmountMinor(), properties.getPatreonUrl());
    }
}
