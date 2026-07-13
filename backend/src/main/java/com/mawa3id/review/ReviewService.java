package com.mawa3id.review;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.review.dto.ReviewRequest;
import com.mawa3id.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public ReviewService(ReviewRepository reviewRepository, AppointmentRepository appointmentRepository) {
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** Leave a review for a completed, patient-owned appointment. */
    @Transactional
    public ReviewResponse create(Long patientUserId, Long appointmentId, ReviewRequest request) {
        Appointment appointment = getOwnedByPatient(appointmentId, patientUserId);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only a completed appointment can be reviewed");
        }
        if (reviewRepository.existsByAppointmentId(appointmentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This appointment already has a review");
        }
        Review saved = reviewRepository.saveAndFlush(
                new Review(appointment, request.rating(), request.comment()));
        recomputeDoctorRating(appointment.getDoctor());
        return ReviewResponse.from(saved);
    }

    @Transactional
    public ReviewResponse update(Long patientUserId, Long appointmentId, ReviewRequest request) {
        Review review = getReview(appointmentId);
        if (!review.getAppointment().getPatient().getUserId().equals(patientUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviewRepository.flush();
        recomputeDoctorRating(review.getAppointment().getDoctor());
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long userId, Long appointmentId) {
        Review review = getReview(appointmentId);
        Appointment appointment = review.getAppointment();
        boolean owner = appointment.getPatient().getUserId().equals(userId)
                || appointment.getDoctor().getUserId().equals(userId);
        if (!owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForDoctor(Long doctorUserId, Pageable pageable) {
        return reviewRepository.findByAppointmentDoctorUserIdOrderByCreatedAtDesc(doctorUserId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForPatient(Long patientUserId, Pageable pageable) {
        return reviewRepository.findByAppointmentPatientUserIdOrderByCreatedAtDesc(patientUserId, pageable)
                .map(ReviewResponse::from);
    }

    /** Refresh the doctor's denormalised rating aggregate from the reviews table. */
    private void recomputeDoctorRating(Doctor doctor) {
        DoctorRatingAggregate aggregate = reviewRepository.aggregateForDoctor(doctor.getUserId());
        long count = aggregate.count() == null ? 0 : aggregate.count();
        double average = aggregate.average() == null ? 0 : aggregate.average();
        doctor.setRatingCount((int) count);
        doctor.setRatingAverage(average);
    }

    private Review getReview(Long appointmentId) {
        return reviewRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No review for appointment: " + appointmentId));
    }

    private Appointment getOwnedByPatient(Long appointmentId, Long patientUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getPatient().getUserId().equals(patientUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        return appointment;
    }
}
