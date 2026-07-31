package com.mawa3id.schedule;

import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.common.ApiException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.schedule.dto.AvailabilityRuleRequest;
import com.mawa3id.schedule.dto.AvailabilityRuleResponse;
import com.mawa3id.schedule.dto.SlotResponse;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorService doctorService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        // Real system-zone clock so "now" matches the LocalDate.now()-based test fixtures.
        scheduleService = new ScheduleService(availabilityRepository, appointmentRepository, doctorService,
                Clock.systemDefaultZone());
    }

    private void noBookings() {
        when(appointmentRepository.findByDoctorUserIdAndStatusInAndStartTimeBetween(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
    }

    private static final LocalDate FUTURE_MONDAY =
            LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    private static final LocalDate PAST_MONDAY =
            LocalDate.now().minusYears(1).with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

    private Doctor doctorWithSlot(int slotMinutes) {
        User user = new User("doc@example.com", "hash", Role.DOCTOR);
        Doctor doctor = new Doctor(user, "Dr Slot", new Specialty("Cardiology", "Heart"),
                "Clinic", "9-17", "+212");
        doctor.setSlotDurationMinutes(slotMinutes);
        return doctor;
    }

    private DoctorAvailability rule(Doctor doctor, DayOfWeek day, String start, String end) {
        return rule(doctor, day, start, end, false);
    }

    private DoctorAvailability rule(Doctor doctor, DayOfWeek day, String start, String end, boolean rdvOnly) {
        return new DoctorAvailability(doctor, day, LocalTime.parse(start), LocalTime.parse(end), rdvOnly);
    }

    @Test
    void slicesAvailabilityIntoWholeSlots() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of(rule(doctor, DayOfWeek.MONDAY, "09:00", "12:00")));
        noBookings();

        List<SlotResponse> slots = scheduleService.computeFreeSlots(1L, FUTURE_MONDAY, FUTURE_MONDAY);

        // 09:00–12:00 at 30 minutes => 6 slots, first 09:00, last 11:30.
        assertThat(slots).hasSize(6);
        assertThat(slots.get(0).start().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(5).start().toLocalTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(slots.get(0).end().toLocalTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void dropsTrailingPartialSlot() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of(rule(doctor, DayOfWeek.MONDAY, "09:00", "09:50")));
        noBookings();

        List<SlotResponse> slots = scheduleService.computeFreeSlots(1L, FUTURE_MONDAY, FUTURE_MONDAY);

        // Only 09:00–09:30 fits; 09:30–10:00 would exceed 09:50 and is dropped.
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).start().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void excludesSlotsInThePast() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of(rule(doctor, DayOfWeek.MONDAY, "09:00", "12:00")));
        noBookings();

        List<SlotResponse> slots = scheduleService.computeFreeSlots(1L, PAST_MONDAY, PAST_MONDAY);

        assertThat(slots).isEmpty();
    }

    @Test
    void returnsNoSlotsWhenNoAvailability() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of());
        noBookings();

        assertThat(scheduleService.computeFreeSlots(1L, FUTURE_MONDAY, FUTURE_MONDAY)).isEmpty();
    }

    @Test
    void rejectsRangeExceedingCap() {
        assertThatThrownBy(() -> scheduleService.computeFreeSlots(
                1L, FUTURE_MONDAY, FUTURE_MONDAY.plusDays(ScheduleService.MAX_RANGE_DAYS + 1)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> scheduleService.computeFreeSlots(
                1L, FUTURE_MONDAY, FUTURE_MONDAY.minusDays(1)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getAvailabilityMapsRulesToResponses() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of(rule(doctor, DayOfWeek.TUESDAY, "08:00", "10:00")));

        List<AvailabilityRuleResponse> rules = scheduleService.getAvailability(1L);

        assertThat(rules).singleElement().satisfies(r -> {
            assertThat(r.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
            assertThat(r.startTime()).isEqualTo(LocalTime.of(8, 0));
        });
    }

    @Test
    void replaceAvailabilityRejectsEndBeforeStart() {
        List<AvailabilityRuleRequest> rules = List.of(new AvailabilityRuleRequest(
                DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0), false));

        assertThatThrownBy(() -> scheduleService.replaceAvailability(1L, rules))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void replaceAvailabilityRejectsOverlapOnSameDay() {
        List<AvailabilityRuleRequest> rules = List.of(
                new AvailabilityRuleRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), false),
                new AvailabilityRuleRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), false));

        assertThatThrownBy(() -> scheduleService.replaceAvailability(1L, rules))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void replaceAvailabilityPersistsValidRules() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        // saveAll echoes back the entities it was given.
        when(availabilityRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<DoctorAvailability>>any()))
                .thenAnswer(inv -> {
                    List<DoctorAvailability> arg = new java.util.ArrayList<>();
                    inv.<Iterable<DoctorAvailability>>getArgument(0).forEach(arg::add);
                    return arg;
                });

        List<AvailabilityRuleResponse> saved = scheduleService.replaceAvailability(1L, List.of(
                new AvailabilityRuleRequest(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), false),
                new AvailabilityRuleRequest(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(17, 0), false)));

        // Response is sorted by day then start time: Monday before Wednesday.
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(saved.get(1).dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    void replaceAvailabilityPersistsRdvOnlyPerRule() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<DoctorAvailability>>any()))
                .thenAnswer(inv -> {
                    List<DoctorAvailability> arg = new java.util.ArrayList<>();
                    inv.<Iterable<DoctorAvailability>>getArgument(0).forEach(arg::add);
                    return arg;
                });

        List<AvailabilityRuleResponse> saved = scheduleService.replaceAvailability(1L, List.of(
                new AvailabilityRuleRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), true),
                new AvailabilityRuleRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), false)));

        assertThat(saved.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(saved.get(0).rdvOnly()).isTrue();
        assertThat(saved.get(1).dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(saved.get(1).rdvOnly()).isFalse();
    }

    @Test
    void rdvOnlyDoesNotAffectSlotGeneration() {
        Doctor doctor = doctorWithSlot(30);
        when(doctorService.getByUserId(1L)).thenReturn(doctor);
        when(availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(1L))
                .thenReturn(List.of(rule(doctor, DayOfWeek.MONDAY, "09:00", "12:00", true)));
        noBookings();

        // An RDV-only window still yields the same six bookable slots as a walk-in one.
        assertThat(scheduleService.computeFreeSlots(1L, FUTURE_MONDAY, FUTURE_MONDAY)).hasSize(6);
    }
}
