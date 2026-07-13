package com.mawa3id.record;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    Page<MedicalRecord> findByAppointmentPatientUserIdOrderByAppointmentStartTimeDesc(
            Long patientUserId, Pageable pageable);
}
