package com.mawa3id.donation.dto;

import com.mawa3id.donation.Donation;
import com.mawa3id.donation.DonationProvider;
import com.mawa3id.donation.DonationStatus;

import java.time.Instant;

/**
 * A donation as returned to clients. {@code checkoutUrl} is only populated on creation
 * (the URL the donor is redirected to); it is {@code null} in history listings.
 */
public record DonationResponse(
        Long id,
        Long userId,
        long amountMinor,
        String currency,
        DonationStatus status,
        DonationProvider provider,
        String donorName,
        String message,
        String checkoutUrl,
        Instant createdAt
) {
    /** History view: no checkout URL. */
    public static DonationResponse from(Donation donation) {
        return from(donation, null);
    }

    /** Creation view: includes the redirect URL for the hosted checkout. */
    public static DonationResponse from(Donation donation, String checkoutUrl) {
        return new DonationResponse(
                donation.getId(),
                donation.getUser() != null ? donation.getUser().getId() : null,
                donation.getAmountMinor(),
                donation.getCurrency(),
                donation.getStatus(),
                donation.getProvider(),
                donation.getDonorName(),
                donation.getMessage(),
                checkoutUrl,
                donation.getCreatedAt());
    }
}
