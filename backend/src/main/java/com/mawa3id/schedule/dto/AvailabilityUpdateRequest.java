package com.mawa3id.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Full replacement of a doctor's weekly availability. An empty list clears it.
 */
public record AvailabilityUpdateRequest(
        @NotNull List<@Valid AvailabilityRuleRequest> rules
) {
}
