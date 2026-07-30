package com.mawa3id.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Sets the caller's preferred language for notifications. */
public record LanguageRequest(
        @NotBlank @Pattern(regexp = "en|fr|ar", message = "language must be one of en, fr, ar")
        String language
) {
}
