package com.mawa3id.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Register (or refresh) a push token for the calling user's device.
 * {@code platform} is a lenient hint ({@code ANDROID}/{@code IOS}); an unknown or missing
 * value is stored as {@code UNKNOWN}.
 */
public record DeviceRegistrationRequest(
        @NotBlank @Size(max = 512) String token,
        String platform) {
}
