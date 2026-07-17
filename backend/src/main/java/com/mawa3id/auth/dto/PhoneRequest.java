package com.mawa3id.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Register (or re-send a code to) a phone number for the authenticated user. */
public record PhoneRequest(
        @NotBlank @Size(max = 32) String phone
) {
}
