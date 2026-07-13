package com.mawa3id.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled-task support (used by the notification reminder job). The job
 * itself is a no-op when {@code mawa3id.notifications.reminder.enabled} is false.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
