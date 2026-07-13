package com.mawa3id.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating or updating a {@link com.mawa3id.record.MedicalRecord}. Only
 * {@code diagnosis} is required; the remaining fields are optional clinical detail.
 */
public record RecordRequest(
        @NotBlank @Size(max = 1000) String diagnosis,
        @Size(max = 4000) String notes,
        @Size(max = 2000) String prescription,
        LocalDate followUpDate
) {
}
