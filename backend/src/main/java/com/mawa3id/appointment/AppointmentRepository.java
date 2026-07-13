package com.mawa3id.appointment;

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

    List<Appointment> findByPatientUserIdOrderByStartTimeDesc(Long patientUserId);

    List<Appointment> findByPatientUserIdAndStatusOrderByStartTimeDesc(
            Long patientUserId, AppointmentStatus status);

    List<Appointment> findByDoctorUserIdOrderByStartTimeAsc(Long doctorUserId);

    List<Appointment> findByDoctorUserIdAndStatusOrderByStartTimeAsc(
            Long doctorUserId, AppointmentStatus status);

    List<Appointment> findByStatusAndStartTimeBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);
}
