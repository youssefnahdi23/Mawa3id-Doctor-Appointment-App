package com.mawa3id.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Periodically emits reminders for upcoming appointments. The scan itself is idempotent
 * (see {@link NotificationService#dispatchDueReminders}); this component only decides when
 * to run and whether the feature is enabled.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final NotificationProperties properties;

    public NotificationScheduler(NotificationService notificationService, NotificationProperties properties) {
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Scheduled(cron = "${mawa3id.notifications.reminder.cron:0 0 * * * *}")
    public void sendDueReminders() {
        if (!properties.getReminder().isEnabled()) {
            return;
        }
        int sent = notificationService.dispatchDueReminders(LocalDateTime.now());
        if (sent > 0) {
            log.info("Dispatched {} appointment reminder notification(s)", sent);
        }
    }
}
