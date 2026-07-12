package com.mawa3id.schedule.dto;

import java.time.LocalDateTime;

/**
 * A single bookable time slot: its start and (exclusive) end.
 */
public record SlotResponse(
        LocalDateTime start,
        LocalDateTime end
) {
}
