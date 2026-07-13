package com.mawa3id.record;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.record.dto.MedicalRecordResponse;
import com.mawa3id.record.dto.RecordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository recordRepository;
    private final AppointmentRepository appointmentRepository;

    public MedicalRecordService(MedicalRecordRepository recordRepository,
                                AppointmentRepository appointmentRepository) {
        this.recordRepository = recordRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Attach a visit record to a doctor-owned appointment. The appointment must be
     * ACCEPTED or COMPLETED; an ACCEPTED appointment is auto-completed as a side effect.
     */
    @Transactional
    public MedicalRecordResponse create(Long doctorUserId, Long appointmentId, RecordRequest request) {
        Appointment appointment = getOwnedByDoctor(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.ACCEPTED
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A visit record can only be attached to an accepted or completed appointment");
        }
        if (recordRepository.existsByAppointmentId(appointmentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This appointment already has a visit record");
        }
        if (appointment.getStatus() == AppointmentStatus.ACCEPTED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
        }
        MedicalRecord saved = recordRepository.save(new MedicalRecord(
                appointment, request.diagnosis(), request.notes(),
                request.prescription(), request.followUpDate()));
        return MedicalRecordResponse.from(saved);
    }

    @Transactional
    public MedicalRecordResponse update(Long doctorUserId, Long appointmentId, RecordRequest request) {
        MedicalRecord record = getRecord(appointmentId);
        if (!record.getAppointment().getDoctor().getUserId().equals(doctorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        record.setDiagnosis(request.diagnosis());
        record.setNotes(request.notes());
        record.setPrescription(request.prescription());
        record.setFollowUpDate(request.followUpDate());
        return MedicalRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse get(Long userId, Long appointmentId) {
        MedicalRecord record = getRecord(appointmentId);
        Appointment appointment = record.getAppointment();
        boolean owner = appointment.getDoctor().getUserId().equals(userId)
                || appointment.getPatient().getUserId().equals(userId);
        if (!owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        return MedicalRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> listForPatient(Long patientUserId, Pageable pageable) {
        return recordRepository
                .findByAppointmentPatientUserIdOrderByAppointmentStartTimeDesc(patientUserId, pageable)
                .map(MedicalRecordResponse::from);
    }

    private MedicalRecord getRecord(Long appointmentId) {
        return recordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No visit record for appointment: " + appointmentId));
    }

    private Appointment getOwnedByDoctor(Long appointmentId, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getDoctor().getUserId().equals(doctorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        return appointment;
    }
}
