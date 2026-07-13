package com.mawa3id.appointment;

import com.mawa3id.appointment.dto.AppointmentResponse;
import com.mawa3id.common.ApiException;
import com.mawa3id.doctor.AcceptanceMode;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.patient.Patient;
import com.mawa3id.patient.PatientService;
import com.mawa3id.schedule.ScheduleService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ScheduleService scheduleService;
    @Mock private DoctorService doctorService;
    @Mock private PatientService patientService;
    @Mock private com.mawa3id.notification.NotificationService notificationService;

    @InjectMocks private AppointmentService appointmentService;

    private static final long DOCTOR_ID = 2L;
    private static final long PATIENT_ID = 1L;
    private static final LocalDateTime START = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);

    private Doctor mockDoctor(AcceptanceMode mode) {
        Doctor doctor = mock(Doctor.class, withSettings().strictness(Strictness.LENIENT));
        when(doctor.getUserId()).thenReturn(DOCTOR_ID);
        when(doctor.getName()).thenReturn("Dr Who");
        when(doctor.getAcceptanceMode()).thenReturn(mode);
        when(doctor.getSlotDurationMinutes()).thenReturn(30);
        return doctor;
    }

    private Patient mockPatient(long id) {
        Patient patient = mock(Patient.class, withSettings().strictness(Strictness.LENIENT));
        when(patient.getUserId()).thenReturn(id);
        when(patient.getFullName()).thenReturn("Alice");
        return patient;
    }

    private Appointment appointment(Doctor doctor, Patient patient, AppointmentStatus status) {
        return new Appointment(doctor, patient, START, START.plusMinutes(30), status, "checkup");
    }

    // ---- booking ----

    @Test
    void bookingManualDoctorCreatesPendingAppointment() {
        Doctor doctor = mockDoctor(AcceptanceMode.MANUAL);
        Patient patient = mockPatient(PATIENT_ID);
        when(doctorService.getByUserId(DOCTOR_ID)).thenReturn(doctor);
        when(patientService.getByUserId(PATIENT_ID)).thenReturn(patient);
        when(scheduleService.isSlotBookable(DOCTOR_ID, START)).thenReturn(true);
        when(appointmentRepository.existsByDoctorUserIdAndStartTimeAndStatusIn(eq(DOCTOR_ID), eq(START), any()))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.book(PATIENT_ID, DOCTOR_ID, START, "checkup");

        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.end()).isEqualTo(START.plusMinutes(30));
    }

    @Test
    void bookingAutoDoctorCreatesAcceptedAppointment() {
        Doctor doctor = mockDoctor(AcceptanceMode.AUTO);
        Patient patient = mockPatient(PATIENT_ID);
        when(doctorService.getByUserId(DOCTOR_ID)).thenReturn(doctor);
        when(patientService.getByUserId(PATIENT_ID)).thenReturn(patient);
        when(scheduleService.isSlotBookable(DOCTOR_ID, START)).thenReturn(true);
        when(appointmentRepository.existsByDoctorUserIdAndStartTimeAndStatusIn(eq(DOCTOR_ID), eq(START), any()))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.book(PATIENT_ID, DOCTOR_ID, START, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.ACCEPTED);
    }

    @Test
    void bookingRejectsSlotOutsideAvailability() {
        Doctor doctor = mockDoctor(AcceptanceMode.MANUAL);
        Patient patient = mockPatient(PATIENT_ID);
        when(doctorService.getByUserId(DOCTOR_ID)).thenReturn(doctor);
        when(patientService.getByUserId(PATIENT_ID)).thenReturn(patient);
        when(scheduleService.isSlotBookable(DOCTOR_ID, START)).thenReturn(false);

        assertThatThrownBy(() -> appointmentService.book(PATIENT_ID, DOCTOR_ID, START, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void bookingRejectsAlreadyBookedSlot() {
        Doctor doctor = mockDoctor(AcceptanceMode.MANUAL);
        Patient patient = mockPatient(PATIENT_ID);
        when(doctorService.getByUserId(DOCTOR_ID)).thenReturn(doctor);
        when(patientService.getByUserId(PATIENT_ID)).thenReturn(patient);
        when(scheduleService.isSlotBookable(DOCTOR_ID, START)).thenReturn(true);
        when(appointmentRepository.existsByDoctorUserIdAndStartTimeAndStatusIn(eq(DOCTOR_ID), eq(START), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.book(PATIENT_ID, DOCTOR_ID, START, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---- accept / reject ----

    @Test
    void acceptMovesPendingToAccepted() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        AppointmentResponse response = appointmentService.accept(DOCTOR_ID, 10L);

        assertThat(response.status()).isEqualTo(AppointmentStatus.ACCEPTED);
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.ACCEPTED);
    }

    @Test
    void rejectMovesPendingToRejected() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThat(appointmentService.reject(DOCTOR_ID, 10L).status())
                .isEqualTo(AppointmentStatus.REJECTED);
    }

    @Test
    void acceptByAnotherDoctorIsForbidden() {
        Doctor otherDoctor = mock(Doctor.class, withSettings().strictness(Strictness.LENIENT));
        when(otherDoctor.getUserId()).thenReturn(999L);
        Appointment appt = appointment(otherDoctor, mockPatient(PATIENT_ID), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> appointmentService.accept(DOCTOR_ID, 10L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void acceptNonPendingIsConflict() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> appointmentService.accept(DOCTOR_ID, 10L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---- cancel ----

    @Test
    void patientCanCancelOwnActiveAppointment() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThat(appointmentService.cancel(PATIENT_ID, 10L).status())
                .isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelByUnrelatedUserIsForbidden() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> appointmentService.cancel(777L, 10L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void cancelAlreadyTerminalIsConflict() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.REJECTED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> appointmentService.cancel(PATIENT_ID, 10L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---- listing ----

    @Test
    void doctorCanCancelOwnAppointment() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

        // Cancelled by the doctor (the second branch of the ownership check).
        assertThat(appointmentService.cancel(DOCTOR_ID, 10L).status())
                .isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void listForPatientMapsToResponses() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findByPatientUserIdOrderByStartTimeDesc(PATIENT_ID))
                .thenReturn(List.of(appt));

        List<AppointmentResponse> result = appointmentService.listForPatient(PATIENT_ID, null);

        assertThat(result).singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(AppointmentStatus.PENDING));
    }

    @Test
    void listForPatientAppliesStatusFilter() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findByPatientUserIdAndStatusOrderByStartTimeDesc(
                PATIENT_ID, AppointmentStatus.ACCEPTED)).thenReturn(List.of(appt));

        assertThat(appointmentService.listForPatient(PATIENT_ID, AppointmentStatus.ACCEPTED)).hasSize(1);
    }

    @Test
    void listForDoctorAppliesStatusFilterAndDefaultsToAll() {
        Appointment appt = appointment(mockDoctor(AcceptanceMode.MANUAL), mockPatient(PATIENT_ID),
                AppointmentStatus.PENDING);
        when(appointmentRepository.findByDoctorUserIdOrderByStartTimeAsc(DOCTOR_ID))
                .thenReturn(List.of(appt));
        when(appointmentRepository.findByDoctorUserIdAndStatusOrderByStartTimeAsc(
                DOCTOR_ID, AppointmentStatus.PENDING)).thenReturn(List.of(appt));

        assertThat(appointmentService.listForDoctor(DOCTOR_ID, null)).hasSize(1);
        assertThat(appointmentService.listForDoctor(DOCTOR_ID, AppointmentStatus.PENDING)).hasSize(1);
    }
}
