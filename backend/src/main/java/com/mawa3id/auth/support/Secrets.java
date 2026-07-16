package com.mawa3id.auth.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic helpers for auth secrets: opaque refresh tokens, numeric OTP codes,
 * and the SHA-256 hashing applied before anything is persisted. Raw secrets are only
 * ever returned to the caller (to be delivered); the database stores hashes.
 */
public final class Secrets {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private Secrets() {
    }

    /** A 256-bit URL-safe opaque token, e.g. for refresh tokens and email links. */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** A zero-padded numeric code of the given length, e.g. a 6-digit SMS OTP. */
    public static String numericCode(int digits) {
        if (digits < 1 || digits > 9) {
            throw new IllegalArgumentException("digits must be between 1 and 9");
        }
        int bound = (int) Math.pow(10, digits);
        int value = RANDOM.nextInt(bound);
        return String.format("%0" + digits + "d", value);
    }

    /** Lowercase SHA-256 hex digest of the given value. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS; this is unreachable on any real JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
