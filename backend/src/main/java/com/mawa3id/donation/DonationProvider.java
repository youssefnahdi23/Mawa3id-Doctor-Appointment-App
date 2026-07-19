package com.mawa3id.donation;

/** Payment provider/channel that processed a donation. */
public enum DonationProvider {
    /** International card via Stripe hosted Checkout. */
    STRIPE,
    /** Tunisian aggregator (D17, Flouci wallet, local bank cards) via Konnect hosted checkout. */
    KONNECT,
    /** Manual bank transfer to the association's RIB; reconciled by hand. */
    RIB
}
