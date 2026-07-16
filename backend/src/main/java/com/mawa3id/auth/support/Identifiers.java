package com.mawa3id.auth.support;

import java.util.regex.Pattern;

/**
 * Normalization and classification of the login/recovery identifiers a user may
 * present: email, phone, or username. Normalization is the single place that decides
 * the canonical form used for uniqueness and lookup, so registration and login agree.
 */
public final class Identifiers {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");
    // Digits, optionally a single leading '+', 6-15 significant digits (loose E.164).
    private static final Pattern PHONE_DIGITS = Pattern.compile("^\\+?[0-9]{6,15}$");

    private Identifiers() {
    }

    public enum Kind { EMAIL, PHONE, USERNAME }

    /** Classify a raw identifier so login knows how to normalize and look it up. */
    public static Kind classify(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.contains("@")) {
            return Kind.EMAIL;
        }
        if (looksLikePhone(trimmed)) {
            return Kind.PHONE;
        }
        return Kind.USERNAME;
    }

    public static boolean isValidEmail(String raw) {
        return raw != null && EMAIL.matcher(raw.trim()).matches();
    }

    public static boolean isValidUsername(String raw) {
        return raw != null && USERNAME.matcher(raw.trim().toLowerCase()).matches();
    }

    private static boolean looksLikePhone(String trimmed) {
        return PHONE_DIGITS.matcher(stripPhoneFormatting(trimmed)).matches();
    }

    /** Lowercase + trim an email into its canonical, comparable form. */
    public static String normalizeEmail(String raw) {
        return raw.trim().toLowerCase();
    }

    /** Lowercase + trim a username into its canonical form. */
    public static String normalizeUsername(String raw) {
        return raw.trim().toLowerCase();
    }

    /**
     * Normalize a phone number to a loose E.164 form: strip spaces, dashes and
     * parentheses, keep a single leading '+'. Full carrier validation is out of scope.
     */
    public static String normalizePhone(String raw) {
        String cleaned = stripPhoneFormatting(raw.trim());
        if (!PHONE_DIGITS.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Not a valid phone number");
        }
        return cleaned;
    }

    private static String stripPhoneFormatting(String value) {
        return value.replaceAll("[\\s()\\-.]", "");
    }

    /**
     * Mask an identifier for logs/audit so it is recognizable but not fully disclosed,
     * e.g. {@code a***e@example.com} or {@code +2126****89}.
     */
    public static String mask(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "";
        }
        int at = identifier.indexOf('@');
        if (at > 0) {
            String local = identifier.substring(0, at);
            String domain = identifier.substring(at);
            return maskMiddle(local) + domain;
        }
        return maskMiddle(identifier);
    }

    private static String maskMiddle(String value) {
        if (value.length() <= 2) {
            return value.charAt(0) + "*";
        }
        int keep = value.length() <= 4 ? 1 : 2;
        String head = value.substring(0, keep);
        String tail = value.substring(value.length() - keep);
        return head + "*".repeat(Math.max(1, value.length() - 2 * keep)) + tail;
    }
}
