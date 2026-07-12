package com.mawa3id.donation;

import com.mawa3id.donation.dto.CreateDonationRequest;
import com.mawa3id.donation.dto.DonationConfigResponse;
import com.mawa3id.donation.dto.DonationResponse;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    /**
     * Start a donation. Public: anonymous when no token is present, otherwise attributed
     * to the authenticated user.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse create(@AuthenticationPrincipal AppUserDetails principal,
                                   @Valid @RequestBody CreateDonationRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        return donationService.create(userId, request);
    }

    /** Stripe webhook callback. Public; authenticity is verified via the signature. */
    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestBody String payload,
                        @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        donationService.handleWebhook(payload, signature);
    }

    /** The authenticated user's own donation history. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<DonationResponse> mine(@AuthenticationPrincipal AppUserDetails principal) {
        return donationService.listForUser(principal.getUserId());
    }

    /** Public donation configuration (card availability + Patreon link). */
    @GetMapping("/config")
    public DonationConfigResponse config() {
        return donationService.config();
    }
}
