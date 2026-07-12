package com.mawa3id.donation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the donations feature, bound from {@code mawa3id.donation.*}.
 * Secrets are supplied via environment variables (see {@code application.yml}); nothing
 * sensitive is committed.
 */
@Component
@ConfigurationProperties(prefix = "mawa3id.donation")
public class DonationProperties {

    /** Master switch for card donations. When false, the create endpoint is rejected. */
    private boolean enabled = true;

    /** Default ISO-4217 currency (lowercase) used when a request omits one. */
    private String currency = "usd";

    /** Minimum accepted amount in minor units (e.g. 100 = 1.00). */
    private long minAmountMinor = 100;

    /** Public Patreon URL surfaced to clients; blank if not configured. */
    private String patreonUrl = "";

    private final Stripe stripe = new Stripe();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getMinAmountMinor() {
        return minAmountMinor;
    }

    public void setMinAmountMinor(long minAmountMinor) {
        this.minAmountMinor = minAmountMinor;
    }

    public String getPatreonUrl() {
        return patreonUrl;
    }

    public void setPatreonUrl(String patreonUrl) {
        this.patreonUrl = patreonUrl;
    }

    public Stripe getStripe() {
        return stripe;
    }

    /** Stripe-specific settings. */
    public static class Stripe {
        private String secretKey = "";
        private String webhookSecret = "";
        private String successUrl = "";
        private String cancelUrl = "";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getSuccessUrl() {
            return successUrl;
        }

        public void setSuccessUrl(String successUrl) {
            this.successUrl = successUrl;
        }

        public String getCancelUrl() {
            return cancelUrl;
        }

        public void setCancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
        }
    }
}
