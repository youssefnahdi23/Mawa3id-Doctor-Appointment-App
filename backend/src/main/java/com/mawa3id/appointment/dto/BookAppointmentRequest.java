package com.mawa3id.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull @Future LocalDateTime startTime,
        @Size(max = 500) String reason
) {
}
