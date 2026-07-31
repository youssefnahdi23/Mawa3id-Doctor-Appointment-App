package com.mawa3id.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Moves an existing appointment to a new start time. */
public record RescheduleRequest(
        @NotNull @Future LocalDateTime startTime
) {
}
