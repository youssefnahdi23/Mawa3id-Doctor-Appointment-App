package com.mawa3id.notification;

/**
 * Published when a {@link Notification} is persisted, so push delivery can be dispatched
 * <em>after</em> the surrounding transaction commits (see {@link PushDispatcher}). Carries
 * only ids/primitives — no JPA entities — so listeners are safe to run post-commit. The
 * {@code title} and {@code body} are already localized to the recipient's language.
 */
public record NotificationCreatedEvent(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long appointmentId) {
}
