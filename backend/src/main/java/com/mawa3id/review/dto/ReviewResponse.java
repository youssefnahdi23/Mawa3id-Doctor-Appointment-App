package com.mawa3id.review.dto;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.review.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long appointmentId,
        Long doctorId,
        Long patientId,
        String patientName,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        Appointment appointment = review.getAppointment();
        return new ReviewResponse(
                review.getId(),
                appointment.getId(),
                appointment.getDoctor().getUserId(),
                appointment.getPatient().getUserId(),
                appointment.getPatient().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
