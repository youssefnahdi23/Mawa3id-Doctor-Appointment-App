package com.mawa3id.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Remove a push token (on logout). */
public record DeviceUnregisterRequest(@NotBlank @Size(max = 512) String token) {
}
