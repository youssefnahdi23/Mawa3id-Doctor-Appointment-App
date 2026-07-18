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
    private final Konnect konnect = new Konnect();
    private final Rib rib = new Rib();

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

    public Konnect getKonnect() {
        return konnect;
    }

    public Rib getRib() {
        return rib;
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

    /**
     * Konnect (Tunisian payment aggregator: D17, Flouci wallet, local cards) settings.
     * The method is offered only when both the API key and receiver wallet id are set.
     */
    public static class Konnect {
        private String apiKey = "";
        private String walletId = "";
        /** Konnect API root; override with the sandbox URL while testing. */
        private String baseUrl = "https://api.konnect.network/api/v2";
        /** ISO-4217 (lowercase). Konnect amounts are in this currency's minor units (TND = millimes). */
        private String currency = "tnd";
        /** Public URL Konnect calls back with ?payment_ref= once a payment settles. */
        private String webhookUrl = "";

        public boolean isConfigured() {
            return !apiKey.isBlank() && !walletId.isBlank();
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWalletId() {
            return walletId;
        }

        public void setWalletId(String walletId) {
            this.walletId = walletId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    /**
     * Direct bank-transfer details (RIB). Purely informational: clients display these and
     * may record a PENDING donation the association reconciles manually.
     */
    public static class Rib {
        private String accountHolder = "";
        private String bankName = "";
        /** The RIB / account number donors transfer to; blank disables the method. */
        private String number = "";
        private String currency = "tnd";

        public boolean isConfigured() {
            return !number.isBlank();
        }

        public String getAccountHolder() {
            return accountHolder;
        }

        public void setAccountHolder(String accountHolder) {
            this.accountHolder = accountHolder;
        }

        public String getBankName() {
            return bankName;
        }

        public void setBankName(String bankName) {
            this.bankName = bankName;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
