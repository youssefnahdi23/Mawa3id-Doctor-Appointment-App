package com.mawa3id.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Start a password reset for whoever owns this identifier (email/username/phone). */
public record ForgotPasswordRequest(
        @NotBlank @Size(max = 255) String identifier
) {
}
