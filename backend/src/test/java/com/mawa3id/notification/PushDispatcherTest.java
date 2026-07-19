package com.mawa3id.notification;

import com.mawa3id.notification.PushSender.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDispatcherTest {

    @Mock private DeviceTokenService deviceTokenService;

    private StubPushSender pushSender;
    private PushDispatcher dispatcher;

    private static final long RECIPIENT = 42L;

    @BeforeEach
    void setUp() {
        pushSender = new StubPushSender();
        dispatcher = new PushDispatcher(deviceTokenService, pushSender);
    }

    private NotificationCreatedEvent event(NotificationType type, Long appointmentId) {
        return new NotificationCreatedEvent(RECIPIENT, type, "You have an update", appointmentId);
    }

    @Test
    void sendsPushToEveryRegisteredToken() {
        when(deviceTokenService.tokensFor(RECIPIENT)).thenReturn(List.of("t1", "t2"));

        dispatcher.onNotificationCreated(event(NotificationType.APPOINTMENT_ACCEPTED, 100L));

        assertThat(pushSender.sent()).hasSize(1);
        StubPushSender.Sent sent = pushSender.sent().get(0);
        assertThat(sent.tokens()).containsExactly("t1", "t2");
        PushMessage message = sent.message();
        assertThat(message.title()).isEqualTo("Appointment confirmed");
        assertThat(message.body()).isEqualTo("You have an update");
        assertThat(message.data())
                .containsEntry("type", "APPOINTMENT_ACCEPTED")
                .containsEntry("appointmentId", "100");
    }

    @Test
    void omitsAppointmentIdFromDataWhenAbsent() {
        when(deviceTokenService.tokensFor(RECIPIENT)).thenReturn(List.of("t1"));

        dispatcher.onNotificationCreated(event(NotificationType.APPOINTMENT_REMINDER, null));

        assertThat(pushSender.sent().get(0).message().data())
                .containsEntry("type", "APPOINTMENT_REMINDER")
                .doesNotContainKey("appointmentId");
    }

    @Test
    void noTokensMeansNoSend() {
        when(deviceTokenService.tokensFor(RECIPIENT)).thenReturn(List.of());

        dispatcher.onNotificationCreated(event(NotificationType.APPOINTMENT_BOOKED, 1L));

        assertThat(pushSender.sent()).isEmpty();
    }

    @Test
    void prunesTokensReportedUnregistered() {
        when(deviceTokenService.tokensFor(RECIPIENT)).thenReturn(List.of("good", "stale"));
        pushSender.markUnregistered("stale");

        dispatcher.onNotificationCreated(event(NotificationType.APPOINTMENT_CANCELLED, 5L));

        verify(deviceTokenService).unregister("stale");
        verify(deviceTokenService, never()).unregister("good");
    }

    @Test
    void swallowsFailuresSoTheDomainTransactionIsUnaffected() {
        when(deviceTokenService.tokensFor(anyLong())).thenThrow(new RuntimeException("db down"));

        // Must not propagate.
        dispatcher.onNotificationCreated(event(NotificationType.APPOINTMENT_REJECTED, 9L));

        assertThat(pushSender.sent()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void everyNotificationTypeHasAPushTitle(NotificationType type) {
        when(deviceTokenService.tokensFor(RECIPIENT)).thenReturn(List.of("t1"));

        dispatcher.onNotificationCreated(event(type, 1L));

        assertThat(pushSender.sent()).hasSize(1);
        assertThat(pushSender.sent().get(0).message().title()).isNotBlank();
    }
}
