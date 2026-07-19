package com.mawa3id.notification;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.notification.dto.NotificationResponse;
import com.mawa3id.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationProperties properties;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notificationRepository,
                               AppointmentRepository appointmentRepository,
                               NotificationProperties properties,
                               ApplicationEventPublisher events) {
        this.notificationRepository = notificationRepository;
        this.appointmentRepository = appointmentRepository;
        this.properties = properties;
        this.events = events;
    }

    /**
     * Persist a single notification. Called from within other services' transactions.
     * Publishes a {@link NotificationCreatedEvent} so push delivery is dispatched after the
     * surrounding transaction commits.
     */
    @Transactional
    public Notification record(User recipient, NotificationType type, String message, Appointment appointment) {
        Notification saved = notificationRepository.save(new Notification(recipient, type, message, appointment));
        events.publishEvent(NotificationCreatedEvent.from(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForUser(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    /** Marks all of the caller's unread notifications read; returns how many were updated. */
    @Transactional
    public int markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadAtIsNull(userId);
        unread.forEach(Notification::markRead);
        return unread.size();
    }

    /**
     * Emit an {@link NotificationType#APPOINTMENT_REMINDER} to both parties of each ACCEPTED
     * appointment starting within the reminder window. Idempotent per appointment.
     */
    @Transactional
    public int dispatchDueReminders(LocalDateTime now) {
        LocalDateTime until = now.plusHours(properties.getReminder().getWindowHours());
        List<Appointment> due = appointmentRepository.findByStatusAndStartTimeBetween(
                AppointmentStatus.ACCEPTED, now, until);
        int sent = 0;
        for (Appointment appointment : due) {
            if (notificationRepository.existsByAppointmentIdAndType(
                    appointment.getId(), NotificationType.APPOINTMENT_REMINDER)) {
                continue;
            }
            String message = "Reminder: your appointment is at " + appointment.getStartTime();
            record(appointment.getPatient().getUser(), NotificationType.APPOINTMENT_REMINDER, message, appointment);
            record(appointment.getDoctor().getUser(), NotificationType.APPOINTMENT_REMINDER, message, appointment);
            sent += 2;
        }
        return sent;
    }
}
