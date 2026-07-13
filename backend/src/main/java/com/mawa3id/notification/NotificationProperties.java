package com.mawa3id.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the notifications feature, bound from {@code mawa3id.notifications.*}.
 * Lifecycle notifications are always emitted; only the scheduled reminder job is configurable.
 */
@Component
@ConfigurationProperties(prefix = "mawa3id.notifications")
public class NotificationProperties {

    private final Reminder reminder = new Reminder();

    public Reminder getReminder() {
        return reminder;
    }

    /** Upcoming-appointment reminder settings. */
    public static class Reminder {
        /** Master switch for the scheduled reminder job. */
        private boolean enabled = true;

        /** How far ahead (hours) an ACCEPTED appointment is reminded about. */
        private int windowHours = 24;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getWindowHours() {
            return windowHours;
        }

        public void setWindowHours(int windowHours) {
            this.windowHours = windowHours;
        }
    }
}
