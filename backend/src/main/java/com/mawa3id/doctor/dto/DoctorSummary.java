package com.mawa3id.doctor.dto;

import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.Governorate;

/**
 * Lightweight projection used in list/search results. Carries {@code specialtyId}
 * so the client can render the specialty label in the active locale from its
 * cached specialties list.
 */
public record DoctorSummary(
        Long userId,
        String name,
        Long specialtyId,
        String specialtyName,
        String cabinetAddress,
        double ratingAverage,
        int ratingCount,
        boolean cnamConventionne,
        Governorate governorate,
        Integer consultationFee
) {
    public static DoctorSummary from(Doctor doctor) {
        return new DoctorSummary(
                doctor.getUserId(),
                doctor.getName(),
                doctor.getSpecialty() == null ? null : doctor.getSpecialty().getId(),
                doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName(),
                doctor.getCabinetAddress(),
                doctor.getRatingAverage(),
                doctor.getRatingCount(),
                doctor.isCnamConventionne(),
                doctor.getGovernorate(),
                doctor.getConsultationFee());
    }
}
