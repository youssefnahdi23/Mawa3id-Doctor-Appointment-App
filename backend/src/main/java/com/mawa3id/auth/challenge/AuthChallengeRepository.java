package com.mawa3id.auth.challenge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, Long> {

    /** Look up a challenge by purpose and the SHA-256 hash of the presented secret. */
    Optional<AuthChallenge> findByPurposeAndTokenHash(ChallengePurpose purpose, String tokenHash);

    /**
     * The most recently issued, not-yet-consumed challenge for a destination. Used to
     * verify short numeric codes, where the presented code cannot be looked up by hash
     * directly without leaking which guesses are close and where per-challenge attempt
     * limiting matters.
     */
    Optional<AuthChallenge> findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
            ChallengePurpose purpose, String destination);
}
