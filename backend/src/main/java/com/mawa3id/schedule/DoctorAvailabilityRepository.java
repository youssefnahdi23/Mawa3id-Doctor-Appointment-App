package com.mawa3id.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorUserIdOrderByDayOfWeekAscStartTimeAsc(Long doctorUserId);

    void deleteByDoctorUserId(Long doctorUserId);
}
