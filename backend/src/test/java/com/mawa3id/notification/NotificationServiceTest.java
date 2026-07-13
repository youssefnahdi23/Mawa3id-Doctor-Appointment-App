package com.mawa3id.notification;

import com.mawa3id.appointment.Appointment;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.notification.dto.NotificationResponse;
import com.mawa3id.patient.Patient;
import com.mawa3id.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private NotificationProperties properties;

    @InjectMocks private NotificationService notificationService;

    private static final long USER_ID = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final NotificationProperties.Reminder reminder = new NotificationProperties.Reminder();

    @BeforeEach
    void configure() {
        reminder.setWindowHours(24);
    }

    private User mockUser(long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().strictness(Strictness.LENIENT));
    }

    private Notification notification(User recipient, boolean read) {
        Notification n = new Notification(recipient, NotificationType.APPOINTMENT_ACCEPTED, "hi", null);
        if (read) {
            n.markRead();
        }
        return n;
    }

    // ---- record ----

    @Test
    void recordPersistsNotification() {
        User recipient = mockUser(USER_ID);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification saved = notificationService.record(
                recipient, NotificationType.APPOINTMENT_BOOKED, "New booking", null);

        assertThat(saved.getType()).isEqualTo(NotificationType.APPOINTMENT_BOOKED);
        verify(notificationRepository).save(any(Notification.class));
    }

    // ---- listing ----

    @Test
    void listForUserAppliesUnreadFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(notification(mockUser(USER_ID), false)));
        when(notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(USER_ID, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.listForUser(USER_ID, true, pageable);

        assertThat(result).hasSize(1);
        verify(notificationRepository, never()).findByRecipientIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listForUserWithoutFilterReturnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(notification(mockUser(USER_ID), true)));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(USER_ID, pageable))
                .thenReturn(page);

        assertThat(notificationService.listForUser(USER_ID, false, pageable)).hasSize(1);
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(USER_ID)).thenReturn(3L);
        assertThat(notificationService.unreadCount(USER_ID)).isEqualTo(3L);
    }

    // ---- mark read ----

    @Test
    void markReadSetsReadAtForOwner() {
        Notification n = notification(mockUser(USER_ID), false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        NotificationResponse response = notificationService.markRead(USER_ID, 5L);

        assertThat(response.read()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markReadByNonOwnerIsForbidden() {
        Notification n = notification(mockUser(USER_ID), false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markRead(999L, 5L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void markReadMissingIsNotFound() {
        when(notificationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(USER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllReadMarksEveryUnread() {
        Notification a = notification(mockUser(USER_ID), false);
        Notification b = notification(mockUser(USER_ID), false);
        when(notificationRepository.findByRecipientIdAndReadAtIsNull(USER_ID)).thenReturn(List.of(a, b));

        assertThat(notificationService.markAllRead(USER_ID)).isEqualTo(2);
        assertThat(a.getReadAt()).isNotNull();
        assertThat(b.getReadAt()).isNotNull();
    }

    // ---- reminders ----

    @Test
    void dispatchDueRemindersNotifiesBothPartiesAndIsIdempotent() {
        when(properties.getReminder()).thenReturn(reminder);
        Appointment appt = dueAppointment(100L);
        when(appointmentRepository.findByStatusAndStartTimeBetween(
                eq(AppointmentStatus.ACCEPTED), eq(NOW), eq(NOW.plusHours(24))))
                .thenReturn(List.of(appt));
        // First run: no reminder yet.
        when(notificationRepository.existsByAppointmentIdAndType(100L, NotificationType.APPOINTMENT_REMINDER))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        int sent = notificationService.dispatchDueReminders(NOW);

        assertThat(sent).isEqualTo(2);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void dispatchDueRemindersSkipsAlreadyReminded() {
        when(properties.getReminder()).thenReturn(reminder);
        Appointment appt = dueAppointment(100L);
        when(appointmentRepository.findByStatusAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(appt));
        when(notificationRepository.existsByAppointmentIdAndType(100L, NotificationType.APPOINTMENT_REMINDER))
                .thenReturn(true);

        assertThat(notificationService.dispatchDueReminders(NOW)).isZero();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private Appointment dueAppointment(long id) {
        User doctorUser = mockUser(2L);
        User patientUser = mockUser(1L);
        Doctor doctor = mock(Doctor.class);
        when(doctor.getUser()).thenReturn(doctorUser);
        Patient patient = mock(Patient.class);
        when(patient.getUser()).thenReturn(patientUser);
        Appointment appt = mock(Appointment.class);
        when(appt.getId()).thenReturn(id);
        when(appt.getDoctor()).thenReturn(doctor);
        when(appt.getPatient()).thenReturn(patient);
        when(appt.getStartTime()).thenReturn(NOW.plusHours(2));
        return appt;
    }
}
