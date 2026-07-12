package com.mawa3id.doctor;

import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.dto.DoctorUpdateRequest;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.specialty.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public DoctorService(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public Page<Doctor> search(Long specialtyId, String q, Pageable pageable) {
        String namePattern = StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase() + "%"
                : null;
        return doctorRepository.search(specialtyId, namePattern, pageable);
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
        if (request.specialtyId() != null) {
            doctor.setSpecialty(resolveSpecialty(request.specialtyId()));
        }
        return doctor;
    }

    @Transactional(readOnly = true)
    public Specialty resolveSpecialty(Long specialtyId) {
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + specialtyId));
    }
}
