package com.mawa3id.notification;

/**
 * Published when a {@link Notification} is persisted, so push delivery can be dispatched
 * <em>after</em> the surrounding transaction commits (see {@link PushDispatcher}). Carries
 * only ids/primitives — no JPA entities — so listeners are safe to run post-commit.
 */
public record NotificationCreatedEvent(
        Long recipientId,
        NotificationType type,
        String message,
        Long appointmentId) {

    static NotificationCreatedEvent from(Notification notification) {
        Long appointmentId = notification.getAppointment() != null
                ? notification.getAppointment().getId()
                : null;
        return new NotificationCreatedEvent(
                notification.getRecipient().getId(),
                notification.getType(),
                notification.getMessage(),
                appointmentId);
    }
}
