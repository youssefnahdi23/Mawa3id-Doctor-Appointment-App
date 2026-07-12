package com.mawa3id.donation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to start a donation. {@code currency} is optional and defaults to the
 * server-configured currency when omitted. The minimum amount is enforced server-side.
 */
public record CreateDonationRequest(
        @NotNull @Positive Long amountMinor,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 120) String donorName,
        @Size(max = 500) String message
) {
}
