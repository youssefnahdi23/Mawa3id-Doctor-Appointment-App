package com.mawa3id.notification;

import com.mawa3id.notification.PushSender.PushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers a push notification for every persisted {@link Notification}, after the
 * transaction that created it commits. Loads the recipient's device tokens, sends via
 * {@link PushSender}, and prunes any tokens the provider reports as unregistered.
 *
 * <p>Push must never break the domain transaction, so failures here are logged and
 * swallowed — the in-app notification has already been committed regardless.
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    public PushDispatcher(DeviceTokenService deviceTokenService, PushSender pushSender) {
        this.deviceTokenService = deviceTokenService;
        this.pushSender = pushSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            List<String> tokens = deviceTokenService.tokensFor(event.recipientId());
            if (tokens.isEmpty()) {
                return;
            }
            PushMessage message = new PushMessage(titleFor(event.type()), event.message(), dataFor(event));
            List<String> unregistered = pushSender.send(tokens, message);
            for (String token : unregistered) {
                deviceTokenService.unregister(token);
            }
        } catch (Exception e) {
            log.warn("Push dispatch failed for notification to user {}: {}",
                    event.recipientId(), e.getMessage());
        }
    }

    private static Map<String, String> dataFor(NotificationCreatedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        if (event.appointmentId() != null) {
            data.put("appointmentId", String.valueOf(event.appointmentId()));
        }
        return data;
    }

    private static String titleFor(NotificationType type) {
        return switch (type) {
            case APPOINTMENT_BOOKED -> "New appointment request";
            case APPOINTMENT_ACCEPTED -> "Appointment confirmed";
            case APPOINTMENT_REJECTED -> "Appointment declined";
            case APPOINTMENT_CANCELLED -> "Appointment cancelled";
            case APPOINTMENT_COMPLETED -> "Appointment completed";
            case APPOINTMENT_REMINDER -> "Appointment reminder";
        };
    }
}
