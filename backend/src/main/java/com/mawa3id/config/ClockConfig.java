package com.mawa3id.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides the clock used for all "now" comparisons on clinic-local times (slot computation,
 * bookability, appointment reminders). Bound to {@code mawa3id.clinic.zone} so the single-region
 * assumption is explicit and configurable rather than relying on the JVM's default time zone.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clinicClock(@Value("${mawa3id.clinic.zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
