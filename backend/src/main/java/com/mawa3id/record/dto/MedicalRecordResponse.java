package com.mawa3id.record.dto;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.record.MedicalRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalRecordResponse(
        Long id,
        Long appointmentId,
        Long doctorId,
        String doctorName,
        Long patientId,
        String patientName,
        LocalDateTime visitStart,
        String diagnosis,
        String notes,
        String prescription,
        LocalDate followUpDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static MedicalRecordResponse from(MedicalRecord record) {
        Appointment appointment = record.getAppointment();
        return new MedicalRecordResponse(
                record.getId(),
                appointment.getId(),
                appointment.getDoctor().getUserId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getUserId(),
                appointment.getPatient().getFullName(),
                appointment.getStartTime(),
                record.getDiagnosis(),
                record.getNotes(),
                record.getPrescription(),
                record.getFollowUpDate(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
