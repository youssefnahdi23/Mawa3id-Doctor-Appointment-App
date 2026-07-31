package com.mawa3id.patient;

import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.patient.dto.PatientUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class PatientService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public Patient getByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    @Transactional
    public Patient update(Long userId, PatientUpdateRequest request) {
        Patient patient = getByUserId(userId);
        patient.setFullName(request.fullName());
        if (request.dateOfBirth() != null) {
            patient.setDateOfBirth(request.dateOfBirth());
        }
        patient.setPhone(request.phone());
        return patient;
    }

    /**
     * Generates a unique human-friendly patient code, e.g. {@code MW-7K3PQ9}.
     */
    public String generateUniquePatientCode() {
        String code;
        do {
            code = "MW-" + randomSegment(6);
        } while (patientRepository.existsByPatientCode(code));
        return code;
    }

    private String randomSegment(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
