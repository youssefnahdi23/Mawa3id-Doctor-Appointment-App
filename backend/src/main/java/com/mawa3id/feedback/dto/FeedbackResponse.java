package com.mawa3id.feedback.dto;

import com.mawa3id.feedback.Feedback;
import com.mawa3id.feedback.FeedbackCategory;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        FeedbackCategory category,
        String message,
        Instant createdAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getCategory(),
                feedback.getMessage(),
                feedback.getCreatedAt());
    }
}
