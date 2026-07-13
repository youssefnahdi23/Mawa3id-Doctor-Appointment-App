package com.mawa3id.review;

/**
 * Aggregate over a doctor's reviews. {@code average} is {@code null} when the doctor has
 * no reviews (SQL {@code AVG} over an empty set).
 */
public record DoctorRatingAggregate(Long count, Double average) {
}
