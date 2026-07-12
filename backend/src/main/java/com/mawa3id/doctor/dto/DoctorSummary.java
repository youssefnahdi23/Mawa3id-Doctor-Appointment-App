package com.mawa3id.doctor.dto;

import com.mawa3id.doctor.Doctor;

/**
 * Lightweight projection used in list/search results.
 */
public record DoctorSummary(
        Long userId,
        String name,
        String specialtyName,
        String cabinetAddress
) {
    public static DoctorSummary from(Doctor doctor) {
        return new DoctorSummary(
                doctor.getUserId(),
                doctor.getName(),
                doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName(),
                doctor.getCabinetAddress());
    }
}
