package com.mawa3id.auth.challenge;

/**
 * Delivers a challenge secret (verification code or reset token) to its destination.
 * This is the seam where a real email/SMS provider is plugged in; the shipped default
 * ({@link LoggingChallengeSender}) either logs the secret for local development or, in
 * production configuration, refuses to deliver so an unconfigured environment fails
 * loudly instead of silently accepting unverifiable challenges.
 */
public interface ChallengeSender {

    /**
     * @param channel     the medium the secret should be delivered over
     * @param destination normalized address (lowercased email / E.164 phone)
     * @param purpose     what the secret authorizes, for message templating
     * @param secret      the raw secret to deliver (never persisted in raw form)
     */
    void send(ChallengeChannel channel, String destination, ChallengePurpose purpose, String secret);
}
