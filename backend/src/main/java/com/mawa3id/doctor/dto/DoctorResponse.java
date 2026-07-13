package com.mawa3id.doctor.dto;

import com.mawa3id.doctor.AcceptanceMode;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.specialty.dto.SpecialtyResponse;

public record DoctorResponse(
        Long userId,
        String email,
        String name,
        SpecialtyResponse specialty,
        String cabinetAddress,
        String workingHours,
        String phone,
        String bio,
        AcceptanceMode acceptanceMode,
        int slotDurationMinutes,
        double ratingAverage,
        int ratingCount
) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
                doctor.getUserId(),
                doctor.getUser().getEmail(),
                doctor.getName(),
                doctor.getSpecialty() == null ? null : SpecialtyResponse.from(doctor.getSpecialty()),
                doctor.getCabinetAddress(),
                doctor.getWorkingHours(),
                doctor.getPhone(),
                doctor.getBio(),
                doctor.getAcceptanceMode(),
                doctor.getSlotDurationMinutes(),
                doctor.getRatingAverage(),
                doctor.getRatingCount());
    }
}
