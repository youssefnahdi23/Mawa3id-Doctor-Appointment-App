package com.mawa3id.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the auth system, bound from {@code mawa3id.auth.*}: refresh-session
 * lifetime, challenge (OTP/link) lifetime and attempt budget, and how challenge
 * secrets are delivered.
 */
@Component
@ConfigurationProperties(prefix = "mawa3id.auth")
public class AuthProperties {

    private final Refresh refresh = new Refresh();
    private final Challenge challenge = new Challenge();
    private final Delivery delivery = new Delivery();

    public Refresh getRefresh() {
        return refresh;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    /** Refresh-token session settings. */
    public static class Refresh {
        /** How long a refresh session remains valid, in days. */
        private long ttlDays = 30;

        public long getTtlDays() {
            return ttlDays;
        }

        public void setTtlDays(long ttlDays) {
            this.ttlDays = ttlDays;
        }
    }

    /** Verification/reset challenge settings. */
    public static class Challenge {
        /** How long a challenge secret remains valid, in seconds. */
        private long ttlSeconds = 900;

        /** How many verification attempts a single challenge tolerates. */
        private int maxAttempts = 5;

        /** Length of numeric OTP codes sent over SMS. */
        private int codeDigits = 6;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getCodeDigits() {
            return codeDigits;
        }

        public void setCodeDigits(int codeDigits) {
            this.codeDigits = codeDigits;
        }
    }

    /** How challenge secrets are delivered until a real email/SMS provider is wired. */
    public static class Delivery {
        public enum Mode {
            /** Log the secret at WARN. Development only — never enable in production. */
            LOG,
            /** Reject delivery with 503 so no unconfigured environment silently "works". */
            DISABLED
        }

        private Mode mode = Mode.LOG;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }
    }
}
