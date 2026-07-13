package com.mawa3id.record;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<MedicalRecord> findByAppointmentPatientUserIdOrderByAppointmentStartTimeDesc(Long patientUserId);
}
