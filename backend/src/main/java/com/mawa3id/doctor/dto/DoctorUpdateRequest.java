package com.mawa3id.doctor.dto;

import com.mawa3id.doctor.AcceptanceMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        Long specialtyId,
        @Size(max = 300) String cabinetAddress,
        @Size(max = 500) String workingHours,
        @Size(max = 40) String phone,
        @Size(max = 1000) String bio,
        AcceptanceMode acceptanceMode
) {
}
