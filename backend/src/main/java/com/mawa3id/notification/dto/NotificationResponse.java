package com.mawa3id.notification.dto;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.notification.Notification;
import com.mawa3id.notification.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Long appointmentId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        Appointment appointment = notification.getAppointment();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                appointment == null ? null : appointment.getId(),
                notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}
