package com.mawa3id.schedule.dto;

import com.mawa3id.schedule.DoctorAvailability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleResponse(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static AvailabilityRuleResponse from(DoctorAvailability availability) {
        return new AvailabilityRuleResponse(
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime());
    }
}
