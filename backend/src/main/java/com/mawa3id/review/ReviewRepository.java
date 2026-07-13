package com.mawa3id.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    Page<Review> findByAppointmentDoctorUserIdOrderByCreatedAtDesc(Long doctorUserId, Pageable pageable);

    List<Review> findByAppointmentPatientUserIdOrderByCreatedAtDesc(Long patientUserId);

    @Query("""
            SELECT new com.mawa3id.review.DoctorRatingAggregate(COUNT(r), AVG(r.rating))
            FROM Review r
            WHERE r.appointment.doctor.userId = :doctorId
            """)
    DoctorRatingAggregate aggregateForDoctor(@Param("doctorId") Long doctorId);
}
