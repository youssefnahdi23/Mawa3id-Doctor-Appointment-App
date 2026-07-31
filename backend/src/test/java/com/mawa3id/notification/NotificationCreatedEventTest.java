package com.mawa3id.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCreatedEventTest {

    @Test
    void carriesLocalizedTitleBodyAndAppointmentId() {
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                7L, NotificationType.APPOINTMENT_BOOKED, "عنوان", "نص", 55L);

        assertThat(event.recipientId()).isEqualTo(7L);
        assertThat(event.type()).isEqualTo(NotificationType.APPOINTMENT_BOOKED);
        assertThat(event.title()).isEqualTo("عنوان");
        assertThat(event.body()).isEqualTo("نص");
        assertThat(event.appointmentId()).isEqualTo(55L);
    }

    @Test
    void appointmentIdMayBeNull() {
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                3L, NotificationType.APPOINTMENT_REMINDER, "t", "b", null);

        assertThat(event.appointmentId()).isNull();
    }
}
