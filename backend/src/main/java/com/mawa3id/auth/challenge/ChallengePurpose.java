package com.mawa3id.auth.challenge;

/** What a short-lived {@code auth_challenges} secret authorizes. */
public enum ChallengePurpose {
    /** Confirm ownership of an email contact (verification link/code). */
    VERIFY_EMAIL,
    /** Confirm ownership of a phone contact (SMS OTP). */
    VERIFY_PHONE,
    /** Password reset initiated via an emailed link/code. */
    RESET_EMAIL,
    /** Password reset initiated via an SMS OTP. */
    RESET_SMS
}
