package com.mawa3id.auth.challenge;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.auth.support.Secrets;
import com.mawa3id.common.ApiException;
import com.mawa3id.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Issues and verifies single-use challenge secrets. Two secret shapes are supported:
 *
 * <ul>
 *   <li><b>Codes</b> — short numeric OTPs delivered over SMS/email and verified by
 *       (purpose, destination) with per-challenge attempt limiting, since their low
 *       entropy makes hash lookup alone brute-forceable.</li>
 *   <li><b>Tokens</b> — high-entropy opaque strings (e.g. reset links) verified by
 *       hash lookup; brute force is infeasible so no attempt budget is needed.</li>
 * </ul>
 *
 * Only SHA-256 hashes are persisted; the raw secret is returned once to the caller for
 * delivery and never stored.
 */
@Service
public class ChallengeService {

    private final AuthChallengeRepository repository;
    private final AuthProperties properties;
    private final Clock clock;

    public ChallengeService(AuthChallengeRepository repository, AuthProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Issue a short numeric code (verified later via {@link #verifyCode}). */
    @Transactional
    public String issueCode(User user, ChallengePurpose purpose, ChallengeChannel channel, String destination) {
        String code = Secrets.numericCode(properties.getChallenge().getCodeDigits());
        persist(user, purpose, channel, destination, code);
        return code;
    }

    /** Issue a high-entropy opaque token (verified later via {@link #verifyToken}). */
    @Transactional
    public String issueToken(User user, ChallengePurpose purpose, ChallengeChannel channel, String destination) {
        String token = Secrets.randomToken();
        persist(user, purpose, channel, destination, token);
        return token;
    }

    private void persist(User user, ChallengePurpose purpose, ChallengeChannel channel,
                         String destination, String secret) {
        Instant expiresAt = clock.instant().plus(properties.getChallenge().getTtlSeconds(), ChronoUnit.SECONDS);
        repository.save(new AuthChallenge(user, purpose, channel, destination,
                Secrets.sha256Hex(secret), expiresAt, properties.getChallenge().getMaxAttempts()));
    }

    /**
     * Verify a numeric code against the latest live challenge for the destination.
     * A wrong code burns an attempt; exhausting the budget invalidates the challenge.
     * On success the challenge is consumed and returned.
     */
    @Transactional
    public AuthChallenge verifyCode(ChallengePurpose purpose, String destination, String presentedCode) {
        Instant now = clock.instant();
        AuthChallenge challenge = repository
                .findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(purpose, destination)
                .filter(c -> !c.isExpired(now) && !c.attemptsExhausted())
                .orElseThrow(ChallengeService::invalid);

        if (!challenge.getTokenHash().equals(Secrets.sha256Hex(presentedCode))) {
            challenge.incrementAttempts();
            repository.save(challenge);
            if (challenge.attemptsExhausted()) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many incorrect attempts. Request a new code.");
            }
            throw invalid();
        }

        challenge.consume(now);
        repository.save(challenge);
        return challenge;
    }

    /** Verify a high-entropy token by hash and consume it. */
    @Transactional
    public AuthChallenge verifyToken(ChallengePurpose purpose, String presentedToken) {
        Instant now = clock.instant();
        AuthChallenge challenge = repository
                .findByPurposeAndTokenHash(purpose, Secrets.sha256Hex(presentedToken))
                .filter(c -> c.isUsable(now))
                .orElseThrow(ChallengeService::invalid);
        challenge.consume(now);
        repository.save(challenge);
        return challenge;
    }

    /** Latest live challenge for a destination, if any (used to throttle re-sends). */
    @Transactional(readOnly = true)
    public Optional<AuthChallenge> activeFor(ChallengePurpose purpose, String destination) {
        Instant now = clock.instant();
        return repository
                .findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(purpose, destination)
                .filter(c -> c.isUsable(now));
    }

    private static ApiException invalid() {
        // Deliberately identical for "no such challenge" and "wrong secret" so the
        // response is not an oracle for which codes/tokens exist.
        return new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
    }
}
