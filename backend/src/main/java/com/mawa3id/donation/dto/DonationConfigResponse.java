package com.mawa3id.donation.dto;

/**
 * Public donation configuration for clients: which methods are available and the details
 * needed to surface each one. String fields are blank when not configured.
 *
 * <ul>
 *   <li>{@code cardEnabled} — international card via Stripe ({@code currency} +
 *       {@code minAmountMinor} apply to it).</li>
 *   <li>{@code konnectEnabled} — Tunisian aggregator (D17 / Flouci / local cards);
 *       amounts are in {@code konnectCurrency} minor units (TND = millimes).</li>
 *   <li>{@code patreonUrl} — recurring support link.</li>
 *   <li>{@code ribNumber} (+ holder/bank) — manual bank transfer details.</li>
 * </ul>
 */
public record DonationConfigResponse(
        boolean cardEnabled,
        String currency,
        long minAmountMinor,
        String patreonUrl,
        boolean konnectEnabled,
        String konnectCurrency,
        String ribAccountHolder,
        String ribBankName,
        String ribNumber,
        String ribCurrency
) {
}
