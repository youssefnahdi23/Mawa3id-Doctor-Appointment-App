package com.mawa3id.record;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.patient.Patient;
import com.mawa3id.record.dto.MedicalRecordResponse;
import com.mawa3id.record.dto.RecordRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository recordRepository;
    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks private MedicalRecordService recordService;

    private static final long DOCTOR_ID = 2L;
    private static final long PATIENT_ID = 1L;
    private static final long APPT_ID = 10L;
    private static final LocalDateTime START = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);

    private final RecordRequest request =
            new RecordRequest("Flu", "Rest and fluids", "Paracetamol 500mg", LocalDate.now().plusDays(7));

    private Doctor mockDoctor(long id) {
        Doctor doctor = mock(Doctor.class, withSettings().strictness(Strictness.LENIENT));
        when(doctor.getUserId()).thenReturn(id);
        when(doctor.getName()).thenReturn("Dr Who");
        return doctor;
    }

    private Patient mockPatient(long id) {
        Patient patient = mock(Patient.class, withSettings().strictness(Strictness.LENIENT));
        when(patient.getUserId()).thenReturn(id);
        when(patient.getFullName()).thenReturn("Alice");
        return patient;
    }

    private Appointment appointment(long doctorId, long patientId, AppointmentStatus status) {
        return new Appointment(mockDoctor(doctorId), mockPatient(patientId), START,
                START.plusMinutes(30), status, "checkup");
    }

    private MedicalRecord record(Appointment appointment) {
        return new MedicalRecord(appointment, "Flu", "Rest", "Paracetamol", null);
    }

    // ---- create ----

    @Test
    void createAttachesRecordAndAutoCompletesAcceptedAppointment() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));
        when(recordRepository.existsByAppointmentId(APPT_ID)).thenReturn(false);
        when(recordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecordResponse response = recordService.create(DOCTOR_ID, APPT_ID, request);

        assertThat(response.diagnosis()).isEqualTo("Flu");
        assertThat(response.doctorId()).isEqualTo(DOCTOR_ID);
        assertThat(response.patientId()).isEqualTo(PATIENT_ID);
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void createOnAlreadyCompletedAppointmentKeepsStatus() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));
        when(recordRepository.existsByAppointmentId(APPT_ID)).thenReturn(false);
        when(recordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        recordService.create(DOCTOR_ID, APPT_ID, request);

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void createByAnotherDoctorIsForbidden() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> recordService.create(999L, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createOnPendingAppointmentIsConflict() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.PENDING);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> recordService.create(DOCTOR_ID, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createOnCancelledAppointmentIsConflict() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> recordService.create(DOCTOR_ID, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createDuplicateRecordIsConflict() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.of(appt));
        when(recordRepository.existsByAppointmentId(APPT_ID)).thenReturn(true);

        assertThatThrownBy(() -> recordService.create(DOCTOR_ID, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createOnMissingAppointmentIsNotFound() {
        when(appointmentRepository.findById(APPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.create(DOCTOR_ID, APPT_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- update ----

    @Test
    void updateRewritesEditableFields() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        MedicalRecord existing = record(appt);
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(existing));

        RecordRequest updated = new RecordRequest("Bronchitis", "Antibiotics", "Amoxicillin", null);
        MedicalRecordResponse response = recordService.update(DOCTOR_ID, APPT_ID, updated);

        assertThat(response.diagnosis()).isEqualTo("Bronchitis");
        assertThat(existing.getPrescription()).isEqualTo("Amoxicillin");
        assertThat(existing.getFollowUpDate()).isNull();
    }

    @Test
    void updateByAnotherDoctorIsForbidden() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(record(appt)));

        assertThatThrownBy(() -> recordService.update(999L, APPT_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- get ----

    @Test
    void doctorOwnerCanReadRecord() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(record(appt)));

        assertThat(recordService.get(DOCTOR_ID, APPT_ID).doctorName()).isEqualTo("Dr Who");
    }

    @Test
    void patientOwnerCanReadRecord() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(record(appt)));

        assertThat(recordService.get(PATIENT_ID, APPT_ID).diagnosis()).isEqualTo("Flu");
    }

    @Test
    void strangerCannotReadRecord() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.of(record(appt)));

        assertThatThrownBy(() -> recordService.get(777L, APPT_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getMissingRecordIsNotFound() {
        when(recordRepository.findByAppointmentId(APPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.get(PATIENT_ID, APPT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- listing ----

    @Test
    void listForPatientMapsToResponses() {
        Appointment appt = appointment(DOCTOR_ID, PATIENT_ID, AppointmentStatus.COMPLETED);
        when(recordRepository.findByAppointmentPatientUserIdOrderByAppointmentStartTimeDesc(PATIENT_ID))
                .thenReturn(List.of(record(appt)));

        assertThat(recordService.listForPatient(PATIENT_ID)).singleElement()
                .satisfies(r -> assertThat(r.diagnosis()).isEqualTo("Flu"));
    }
}
