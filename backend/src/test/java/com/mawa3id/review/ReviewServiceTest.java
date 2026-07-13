package com.mawa3id.review;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.patient.Patient;
import com.mawa3id.review.dto.ReviewRequest;
import com.mawa3id.review.dto.ReviewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks private ReviewService reviewService;

    private static final long DOCTOR_ID = 2L;
    private static final long PATIENT_ID = 1L;
    private static final long APPT_ID = 10L;
    private static final LocalDateTime START = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);

    private final ReviewRequest request = new ReviewRequest(5, "Great doctor");

    private Doctor doctor;

    private Doctor mockDoctor() {
        Doctor d = mock(Doctor.class, withSettings().strictness(Strictness.LENIENT));
        when(d.getUserId()).thenReturn(DOCTOR_ID);
        return d;
    }

    private Patient mockPatient(long id) {
        Patient patient = mock(Patient.class, withSettings().strictness(Strictness.LENIENT));
        when(patient.getUserId()).thenReturn(id);
        when(patient.getFullName()).thenReturn("Alice");
        return patient;
    }

    private Appointment appointment(long patientId, AppointmentStatus status) {
        doctor = mockDoctor();
        return new Appointment(doctor, mockPatient(patientId), START, START.plusMinutes(30), status, "checkup");
    }

    private Review review(Appointment appointment) {
        return new Review(appointment, 5, "Great doctor");
    }

    // ---- create ----

    @Test
    void createStoresReviewAndRecomputesDoctorAggregate() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));
        when(reviewRepository.existsByAppointmentId(APPT_ID)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.aggregateForDoctor(DOCTOR_ID))
                .thenReturn(new DoctorRatingAggregate(1L, 5.0));

        ReviewResponse response = reviewService.create(PATIENT_ID, APPT_ID, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.patientId()).isEqualTo(PATIENT_ID);
        verify(doctor).setRatingCount(1);
        verify(doctor).setRatingAverage(5.0);
    }

    @Test
    void createByAnotherPatientIsForbidden() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> reviewService.create(999L, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createOnNonCompletedAppointmentIsConflict() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> reviewService.create(PATIENT_ID, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createDuplicateReviewIsConflict() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));
        when(reviewRepository.existsByAppointmentId(APPT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(PATIENT_ID, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createOnMissingAppointmentIsNotFound() {
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(PATIENT_ID, APPT_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- update ----

    @Test
    void updateRewritesRatingAndRecomputes() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        Review existing = review(appt);
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.aggregateForDoctor(DOCTOR_ID))
                .thenReturn(new DoctorRatingAggregate(1L, 2.0));

        ReviewResponse response = reviewService.update(PATIENT_ID, APPT_ID, new ReviewRequest(2, "Meh"));

        assertThat(response.rating()).isEqualTo(2);
        assertThat(existing.getComment()).isEqualTo("Meh");
        verify(doctor).setRatingAverage(2.0);
    }

    @Test
    void updateByAnotherPatientIsForbidden() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(review(appt)));

        assertThatThrownBy(() -> reviewService.update(999L, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- get ----

    @Test
    void patientOwnerCanReadReview() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(review(appt)));

        assertThat(reviewService.get(PATIENT_ID, APPT_ID).rating()).isEqualTo(5);
    }

    @Test
    void doctorOwnerCanReadReview() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(review(appt)));

        assertThat(reviewService.get(DOCTOR_ID, APPT_ID).patientName()).isEqualTo("Alice");
    }

    @Test
    void strangerCannotReadReview() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(review(appt)));

        assertThatThrownBy(() -> reviewService.get(777L, APPT_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getMissingReviewIsNotFound() {
        when(reviewRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.get(PATIENT_ID, APPT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- listing ----

    @Test
    void listForPatientMapsToResponses() {
        Appointment appt = appointment(PATIENT_ID, AppointmentStatus.COMPLETED);
        when(reviewRepository.findByAppointmentPatientUserIdOrderByCreatedAtDesc(PATIENT_ID))
                .thenReturn(List.of(review(appt)));

        assertThat(reviewService.listForPatient(PATIENT_ID)).singleElement()
                .satisfies(r -> assertThat(r.rating()).isEqualTo(5));
    }
}
