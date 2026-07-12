package com.mawa3id.appointment.dto;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        Long patientId,
        String patientName,
        LocalDateTime start,
        LocalDateTime end,
        AppointmentStatus status,
        String reason
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctor().getUserId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getUserId(),
                appointment.getPatient().getFullName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason());
    }
}
