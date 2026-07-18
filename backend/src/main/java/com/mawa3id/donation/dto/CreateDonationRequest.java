package com.mawa3id.donation.dto;

import com.mawa3id.donation.DonationProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to start a donation. {@code provider} selects the payment channel and defaults
 * to {@link DonationProvider#STRIPE} for backwards compatibility. {@code currency} is
 * optional and defaults to the provider's configured currency when omitted; the minimum
 * amount is enforced server-side.
 */
public record CreateDonationRequest(
        @NotNull @Positive Long amountMinor,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 120) String donorName,
        @Size(max = 500) String message,
        DonationProvider provider
) {
    /** The requested provider, defaulting to Stripe when the field is omitted. */
    public DonationProvider providerOrDefault() {
        return provider != null ? provider : DonationProvider.STRIPE;
    }
}
