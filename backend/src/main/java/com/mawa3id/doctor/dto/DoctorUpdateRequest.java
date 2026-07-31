package com.mawa3id.doctor.dto;

import com.mawa3id.doctor.AcceptanceMode;
import com.mawa3id.doctor.Governorate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DoctorUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        Long specialtyId,
        @Size(max = 300) String cabinetAddress,
        @Size(max = 500) String workingHours,
        @Size(max = 40) String phone,
        @Size(max = 1000) String bio,
        AcceptanceMode acceptanceMode,
        @Min(5) @Max(240) Integer slotDurationMinutes,
        Boolean cnamConventionne,
        Governorate governorate,
        @Min(0) @Max(100000) Integer consultationFee,
        List<@Size(max = 8) String> languages
) {
}
