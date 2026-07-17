package com.mawa3id.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDoctorRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 200) String name,
        @NotNull Long specialtyId,
        @Size(max = 300) String cabinetAddress,
        @Size(max = 500) String workingHours,
        @Size(max = 40) String phone,
        // Optional: a chosen handle; blank/absent falls back to a generated one.
        @Size(max = 30) String username
) {
}
