package com.mawa3id.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPhoneConfirmRequest(
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(max = 12) String code
) {
}
