package com.mawa3id.auth.challenge;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.auth.support.Identifiers;
import com.mawa3id.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Default {@link ChallengeSender} used until a real email/SMS integration is wired.
 *
 * <ul>
 *   <li>{@code LOG} mode (default, development): logs the raw secret at WARN so flows
 *       can be exercised end-to-end locally.</li>
 *   <li>{@code DISABLED} mode (production): refuses to deliver and surfaces a clear
 *       503 so a misconfigured deployment does not silently issue undeliverable
 *       challenges.</li>
 * </ul>
 *
 * A production integration replaces this bean with one backed by an SMTP/SMS provider.
 * Excluded from the {@code test} profile, where a capturing sender is used instead.
 */
@Component
@Profile("!test")
public class LoggingChallengeSender implements ChallengeSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingChallengeSender.class);

    private final AuthProperties properties;

    public LoggingChallengeSender(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(ChallengeChannel channel, String destination, ChallengePurpose purpose, String secret) {
        AuthProperties.Delivery.Mode mode = properties.getDelivery().getMode();
        if (mode == AuthProperties.Delivery.Mode.DISABLED) {
            log.error("Challenge delivery is DISABLED but a {} secret was requested for {} via {}. "
                            + "Configure a real ChallengeSender or set mawa3id.auth.delivery.mode=log.",
                    purpose, Identifiers.mask(destination), channel);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification delivery is not configured on this server");
        }
        // LOG mode — development only. The mask keeps the destination out of logs; the
        // secret is logged deliberately so it can be copied during local testing.
        log.warn("[DEV challenge] purpose={} channel={} destination={} secret={} "
                        + "(delivery is in LOG mode; do not use in production)",
                purpose, channel, Identifiers.mask(destination), secret);
    }
}
