package com.mawa3id.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientUpdateRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Past LocalDate dateOfBirth
) {
}
