package com.mawa3id.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        // "RDVs Only" (no walk-ins). Optional for backward compatibility; absent → false.
        boolean rdvOnly
) {
}
