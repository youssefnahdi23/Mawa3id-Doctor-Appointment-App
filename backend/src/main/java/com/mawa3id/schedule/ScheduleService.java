package com.mawa3id.schedule;

import com.mawa3id.appointment.AppointmentStatus;
import com.mawa3id.appointment.AppointmentRepository;
import com.mawa3id.common.ApiException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.schedule.dto.AvailabilityRuleRequest;
import com.mawa3id.schedule.dto.AvailabilityRuleResponse;
import com.mawa3id.schedule.dto.SlotResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes bookable slots from a doctor's weekly availability and the doctor's
 * configured slot duration. Times are clinic-local; "now" is evaluated in the configured
 * clinic zone ({@code mawa3id.clinic.zone}) via the injected {@link Clock}, not the JVM default.
 */
@Service
public class ScheduleService {

    /** Maximum span (inclusive) allowed for a slot query, to bound the response size. */
    static final int MAX_RANGE_DAYS = 31;

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final Clock clock;

    public ScheduleService(DoctorAvailabilityRepository availabilityRepository,
                           AppointmentRepository appointmentRepository, DoctorService doctorService,
                           Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> getAvailability(Long doctorUserId) {
        doctorService.getByUserId(doctorUserId); // 404 if the doctor does not exist
        return availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(doctorUserId)
                .stream().map(AvailabilityRuleResponse::from).toList();
    }

    @Transactional
    public List<AvailabilityRuleResponse> replaceAvailability(Long doctorUserId,
                                                              List<AvailabilityRuleRequest> rules) {
        validate(rules);
        Doctor doctor = doctorService.getByUserId(doctorUserId);
        availabilityRepository.deleteByDoctorUserId(doctorUserId);
        List<DoctorAvailability> saved = availabilityRepository.saveAll(rules.stream()
                .map(r -> new DoctorAvailability(doctor, r.dayOfWeek(), r.startTime(), r.endTime()))
                .toList());
        return saved.stream()
                .sorted(Comparator.comparing(DoctorAvailability::getDayOfWeek)
                        .thenComparing(DoctorAvailability::getStartTime))
                .map(AvailabilityRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> computeFreeSlots(Long doctorUserId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'from' and 'to' dates are required");
        }
        if (to.isBefore(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        Doctor doctor = doctorService.getByUserId(doctorUserId);
        int slotMinutes = doctor.getSlotDurationMinutes();
        Map<DayOfWeek, List<DoctorAvailability>> rulesByDay = groupByDay(doctorUserId);
        Set<LocalDateTime> booked = bookedSlotStarts(doctorUserId, from, to);
        LocalDateTime now = LocalDateTime.now(clock);

        List<SlotResponse> slots = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (DoctorAvailability rule : rulesByDay.getOrDefault(date.getDayOfWeek(), List.of())) {
                for (LocalTime start : sliceSlots(rule, slotMinutes)) {
                    LocalDateTime slotStart = LocalDateTime.of(date, start);
                    // A slot is free if it is in the future and not already booked.
                    if (slotStart.isAfter(now) && !booked.contains(slotStart)) {
                        slots.add(new SlotResponse(slotStart, slotStart.plusMinutes(slotMinutes)));
                    }
                }
            }
        }
        return slots;
    }

    /**
     * Whether {@code startTime} is a real, future slot of the doctor: aligned to one of the
     * doctor's availability windows sliced by their slot duration. Booking conflicts are
     * checked separately by the appointment service.
     */
    @Transactional(readOnly = true)
    public boolean isSlotBookable(Long doctorUserId, LocalDateTime startTime) {
        if (startTime == null || !startTime.isAfter(LocalDateTime.now(clock))) {
            return false;
        }
        Doctor doctor = doctorService.getByUserId(doctorUserId);
        int slotMinutes = doctor.getSlotDurationMinutes();
        for (DoctorAvailability rule :
                groupByDay(doctorUserId).getOrDefault(startTime.getDayOfWeek(), List.of())) {
            if (sliceSlots(rule, slotMinutes).contains(startTime.toLocalTime())) {
                return true;
            }
        }
        return false;
    }

    private Set<LocalDateTime> bookedSlotStarts(Long doctorUserId, LocalDate from, LocalDate to) {
        Set<LocalDateTime> booked = new HashSet<>();
        appointmentRepository.findByDoctorUserIdAndStatusInAndStartTimeBetween(
                        doctorUserId, AppointmentStatus.ACTIVE, from.atStartOfDay(), to.atTime(LocalTime.MAX))
                .forEach(a -> booked.add(a.getStartTime()));
        return booked;
    }

    /** Cuts an availability window into whole slot-duration chunks (dropping any remainder). */
    private List<LocalTime> sliceSlots(DoctorAvailability rule, int slotMinutes) {
        int startMin = rule.getStartTime().toSecondOfDay() / 60;
        int endMin = rule.getEndTime().toSecondOfDay() / 60;
        List<LocalTime> starts = new ArrayList<>();
        for (int m = startMin; m + slotMinutes <= endMin; m += slotMinutes) {
            starts.add(LocalTime.ofSecondOfDay(m * 60L));
        }
        return starts;
    }

    private Map<DayOfWeek, List<DoctorAvailability>> groupByDay(Long doctorUserId) {
        Map<DayOfWeek, List<DoctorAvailability>> byDay = new EnumMap<>(DayOfWeek.class);
        for (DoctorAvailability rule :
                availabilityRepository.findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(doctorUserId)) {
            byDay.computeIfAbsent(rule.getDayOfWeek(), d -> new ArrayList<>()).add(rule);
        }
        return byDay;
    }

    private void validate(List<AvailabilityRuleRequest> rules) {
        Map<DayOfWeek, List<AvailabilityRuleRequest>> byDay = new EnumMap<>(DayOfWeek.class);
        for (AvailabilityRuleRequest rule : rules) {
            if (!rule.endTime().isAfter(rule.startTime())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "endTime must be after startTime for " + rule.dayOfWeek());
            }
            byDay.computeIfAbsent(rule.dayOfWeek(), d -> new ArrayList<>()).add(rule);
        }
        for (List<AvailabilityRuleRequest> dayRules : byDay.values()) {
            dayRules.sort(Comparator.comparing(AvailabilityRuleRequest::startTime));
            for (int i = 1; i < dayRules.size(); i++) {
                if (dayRules.get(i).startTime().isBefore(dayRules.get(i - 1).endTime())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Overlapping availability on " + dayRules.get(i).dayOfWeek());
                }
            }
        }
    }
}
