package com.mawa3id.patient.dto;

import com.mawa3id.patient.Patient;

import java.time.LocalDate;

public record PatientResponse(
        Long userId,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        String patientCode
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getUserId(),
                patient.getUser().getEmail(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getPatientCode());
    }
}
