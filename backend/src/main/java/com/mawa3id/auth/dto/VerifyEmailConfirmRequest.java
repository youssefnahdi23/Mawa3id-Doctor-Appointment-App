package com.mawa3id.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailConfirmRequest(
        @NotBlank @Size(max = 12) String code
) {
}
