package com.mawa3id.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a {@link com.mawa3id.review.Review}: a 1–5 star rating
 * and an optional free-text comment.
 */
public record ReviewRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 2000) String comment
) {
}
