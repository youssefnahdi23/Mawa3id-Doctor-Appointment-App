package com.mawa3id.notification;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationCreatedEventTest {

    @Test
    void carriesAppointmentIdWhenPresent() {
        User recipient = mock(User.class);
        when(recipient.getId()).thenReturn(7L);
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(55L);
        Notification notification = new Notification(
                recipient, NotificationType.APPOINTMENT_BOOKED, "hi", appointment);

        NotificationCreatedEvent event = NotificationCreatedEvent.from(notification);

        assertThat(event.recipientId()).isEqualTo(7L);
        assertThat(event.type()).isEqualTo(NotificationType.APPOINTMENT_BOOKED);
        assertThat(event.message()).isEqualTo("hi");
        assertThat(event.appointmentId()).isEqualTo(55L);
    }

    @Test
    void appointmentIdIsNullWhenAbsent() {
        User recipient = mock(User.class);
        when(recipient.getId()).thenReturn(3L);
        Notification notification = new Notification(
                recipient, NotificationType.APPOINTMENT_REMINDER, "later", null);

        NotificationCreatedEvent event = NotificationCreatedEvent.from(notification);

        assertThat(event.appointmentId()).isNull();
    }
}
