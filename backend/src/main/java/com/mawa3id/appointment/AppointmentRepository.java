package com.mawa3id.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorUserIdAndStartTimeAndStatusIn(
            Long doctorUserId, LocalDateTime startTime, Collection<AppointmentStatus> statuses);

    List<Appointment> findByDoctorUserIdAndStatusInAndStartTimeBetween(
            Long doctorUserId, Collection<AppointmentStatus> statuses,
            LocalDateTime from, LocalDateTime to);

    Page<Appointment> findByPatientUserIdOrderByStartTimeDesc(Long patientUserId, Pageable pageable);

    Page<Appointment> findByPatientUserIdAndStatusOrderByStartTimeDesc(
            Long patientUserId, AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDoctorUserIdOrderByStartTimeAsc(Long doctorUserId, Pageable pageable);

    Page<Appointment> findByDoctorUserIdAndStatusOrderByStartTimeAsc(
            Long doctorUserId, AppointmentStatus status, Pageable pageable);

    List<Appointment> findByStatusAndStartTimeBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);
}
