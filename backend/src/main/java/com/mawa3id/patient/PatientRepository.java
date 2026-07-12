package com.mawa3id.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByPatientCode(String patientCode);

    Optional<Patient> findByUserId(Long userId);
}
