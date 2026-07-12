package com.mawa3id.donation.dto;

/**
 * Public donation configuration for clients: whether card donations are available and the
 * Patreon link to surface (empty when not configured).
 */
public record DonationConfigResponse(
        boolean cardEnabled,
        String currency,
        long minAmountMinor,
        String patreonUrl
) {
}
