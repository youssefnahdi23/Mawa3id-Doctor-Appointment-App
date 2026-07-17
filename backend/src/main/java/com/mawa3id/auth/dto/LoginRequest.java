package com.mawa3id.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login by any registered identifier — email, username, or phone number — plus the
 * password. An optional device label is stored on the refresh session so users can
 * recognize their sessions.
 */
public record LoginRequest(
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank String password,
        @Size(max = 120) String deviceName
) {
}
