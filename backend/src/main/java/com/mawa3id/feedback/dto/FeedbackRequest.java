package com.mawa3id.feedback.dto;

import com.mawa3id.feedback.FeedbackCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for submitting app feedback. {@code appVersion} and {@code platform}
 * are optional client-supplied metadata used for triage.
 */
public record FeedbackRequest(
        @NotNull FeedbackCategory category,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 40) String appVersion,
        @Size(max = 20) String platform
) {
}
