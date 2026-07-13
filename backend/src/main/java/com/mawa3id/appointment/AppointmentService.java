package com.mawa3id.appointment;

import com.mawa3id.appointment.dto.AppointmentResponse;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.AcceptanceMode;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.notification.NotificationService;
import com.mawa3id.notification.NotificationType;
import com.mawa3id.patient.Patient;
import com.mawa3id.patient.PatientService;
import com.mawa3id.schedule.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ScheduleService scheduleService;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository, ScheduleService scheduleService,
                              DoctorService doctorService, PatientService patientService,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleService = scheduleService;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AppointmentResponse book(Long patientUserId, Long doctorId, LocalDateTime startTime, String reason) {
        Doctor doctor = doctorService.getByUserId(doctorId);
        Patient patient = patientService.getByUserId(patientUserId);

        if (!scheduleService.isSlotBookable(doctorId, startTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The requested time is not an open slot for this doctor");
        }
        if (appointmentRepository.existsByDoctorUserIdAndStartTimeAndStatusIn(
                doctorId, startTime, AppointmentStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "That slot is already booked");
        }

        AppointmentStatus status = doctor.getAcceptanceMode() == AcceptanceMode.AUTO
                ? AppointmentStatus.ACCEPTED
                : AppointmentStatus.PENDING;
        LocalDateTime endTime = startTime.plusMinutes(doctor.getSlotDurationMinutes());
        Appointment saved = appointmentRepository.save(
                new Appointment(doctor, patient, startTime, endTime, status, reason));

        String note = status == AppointmentStatus.ACCEPTED
                ? "New appointment booked with " + patient.getFullName() + " at " + startTime
                : "New appointment request from " + patient.getFullName() + " at " + startTime;
        notificationService.record(doctor.getUser(), NotificationType.APPOINTMENT_BOOKED, note, saved);
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse accept(Long doctorUserId, Long appointmentId) {
        Appointment appointment = transition(doctorUserId, appointmentId, AppointmentStatus.ACCEPTED);
        notificationService.record(appointment.getPatient().getUser(), NotificationType.APPOINTMENT_ACCEPTED,
                "Your appointment at " + appointment.getStartTime() + " was accepted", appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse reject(Long doctorUserId, Long appointmentId) {
        Appointment appointment = transition(doctorUserId, appointmentId, AppointmentStatus.REJECTED);
        notificationService.record(appointment.getPatient().getUser(), NotificationType.APPOINTMENT_REJECTED,
                "Your appointment at " + appointment.getStartTime() + " was rejected", appointment);
        return AppointmentResponse.from(appointment);
    }

    /** Close out a visit: doctor-owned, only from ACCEPTED. */
    @Transactional
    public AppointmentResponse complete(Long doctorUserId, Long appointmentId) {
        Appointment appointment = getOwnedByDoctor(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only an accepted appointment can be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        notificationService.record(appointment.getPatient().getUser(), NotificationType.APPOINTMENT_COMPLETED,
                "Your visit at " + appointment.getStartTime() + " was completed", appointment);
        return AppointmentResponse.from(appointment);
    }

    /** Accept/reject: doctor-owned, only from PENDING. Returns the mutated appointment. */
    private Appointment transition(Long doctorUserId, Long appointmentId, AppointmentStatus target) {
        Appointment appointment = getOwnedByDoctor(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only a pending appointment can be " + target.name().toLowerCase());
        }
        appointment.setStatus(target);
        return appointment;
    }

    @Transactional
    public AppointmentResponse cancel(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        boolean owner = appointment.getPatient().getUserId().equals(userId)
                || appointment.getDoctor().getUserId().equals(userId);
        if (!owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        if (!AppointmentStatus.ACTIVE.contains(appointment.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only pending or accepted appointments can be cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);

        // Notify the party who did not cancel.
        boolean cancelledByPatient = appointment.getPatient().getUserId().equals(userId);
        var recipient = cancelledByPatient
                ? appointment.getDoctor().getUser()
                : appointment.getPatient().getUser();
        String who = cancelledByPatient ? appointment.getPatient().getFullName() : appointment.getDoctor().getName();
        notificationService.record(recipient, NotificationType.APPOINTMENT_CANCELLED,
                "Appointment at " + appointment.getStartTime() + " was cancelled by " + who, appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listForPatient(Long patientUserId, AppointmentStatus status) {
        List<Appointment> appointments = status == null
                ? appointmentRepository.findByPatientUserIdOrderByStartTimeDesc(patientUserId)
                : appointmentRepository.findByPatientUserIdAndStatusOrderByStartTimeDesc(patientUserId, status);
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listForDoctor(Long doctorUserId, AppointmentStatus status) {
        List<Appointment> appointments = status == null
                ? appointmentRepository.findByDoctorUserIdOrderByStartTimeAsc(doctorUserId)
                : appointmentRepository.findByDoctorUserIdAndStatusOrderByStartTimeAsc(doctorUserId, status);
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    private Appointment getOwnedByDoctor(Long appointmentId, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getDoctor().getUserId().equals(doctorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
        }
        return appointment;
    }
}
