package com.mawa3id.doctor;

import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.dto.DoctorUpdateRequest;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.specialty.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public DoctorService(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public Page<Doctor> search(Long specialtyId, String q, Boolean cnam,
                               Governorate governorate, Pageable pageable) {
        String namePattern = StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase() + "%"
                : null;
        return doctorRepository.search(specialtyId, namePattern, cnam, governorate, pageable);
    }

    @Transactional(readOnly = true)
    public Doctor getByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + userId));
    }

    @Transactional
    public Doctor update(Long userId, DoctorUpdateRequest request) {
        Doctor doctor = getByUserId(userId);
        doctor.setName(request.name());
        doctor.setCabinetAddress(request.cabinetAddress());
        doctor.setWorkingHours(request.workingHours());
        doctor.setPhone(request.phone());
        doctor.setBio(request.bio());
        if (request.acceptanceMode() != null) {
            doctor.setAcceptanceMode(request.acceptanceMode());
        }
        if (request.slotDurationMinutes() != null) {
            doctor.setSlotDurationMinutes(request.slotDurationMinutes());
        }
        if (request.specialtyId() != null) {
            doctor.setSpecialty(resolveSpecialty(request.specialtyId()));
        }
        if (request.cnamConventionne() != null) {
            doctor.setCnamConventionne(request.cnamConventionne());
        }
        doctor.setGovernorate(request.governorate());
        doctor.setConsultationFee(request.consultationFee());
        if (request.languages() != null) {
            doctor.setLanguages(normalizeLanguages(request.languages()));
        }
        return doctor;
    }

    /**
     * Validates each code against {@link SpokenLanguage}, dedupes, and joins to the
     * comma-separated storage form. An empty/blank list clears the field.
     */
    private String normalizeLanguages(java.util.List<String> languages) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : languages) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            try {
                normalized.add(SpokenLanguage.valueOf(code).name());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported language: " + raw);
            }
        }
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    @Transactional(readOnly = true)
    public Specialty resolveSpecialty(Long specialtyId) {
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + specialtyId));
    }
}
