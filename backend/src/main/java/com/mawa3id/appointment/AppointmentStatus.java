package com.mawa3id.appointment;

import java.util.Set;

public enum AppointmentStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    /** Statuses that occupy a slot and therefore block another booking of the same time. */
    public static final Set<AppointmentStatus> ACTIVE = Set.of(PENDING, ACCEPTED);
}
