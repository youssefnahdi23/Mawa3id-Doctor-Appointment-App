package com.mawa3id.auth;

import com.mawa3id.auth.challenge.ChallengeChannel;
import com.mawa3id.auth.challenge.ChallengePurpose;
import com.mawa3id.auth.challenge.ChallengeSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Test-profile {@link ChallengeSender} that captures the last secret it was asked to
 * deliver, so integration tests can complete verification/reset flows end-to-end (the
 * raw secret is never persisted or returned by the API).
 */
@Component
@Profile("test")
public class CapturingChallengeSender implements ChallengeSender {

    private volatile ChallengeChannel lastChannel;
    private volatile String lastDestination;
    private volatile ChallengePurpose lastPurpose;
    private volatile String lastSecret;

    @Override
    public void send(ChallengeChannel channel, String destination, ChallengePurpose purpose, String secret) {
        this.lastChannel = channel;
        this.lastDestination = destination;
        this.lastPurpose = purpose;
        this.lastSecret = secret;
    }

    public ChallengeChannel lastChannel() {
        return lastChannel;
    }

    public String lastDestination() {
        return lastDestination;
    }

    public ChallengePurpose lastPurpose() {
        return lastPurpose;
    }

    public String lastSecret() {
        return lastSecret;
    }

    public void reset() {
        lastChannel = null;
        lastDestination = null;
        lastPurpose = null;
        lastSecret = null;
    }
}
