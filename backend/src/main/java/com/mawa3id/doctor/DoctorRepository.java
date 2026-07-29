package com.mawa3id.doctor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    /**
     * The name filter is passed as a pre-built, already-lowercased LIKE pattern
     * (e.g. {@code %skin%}) or {@code null}. Building it in Java avoids calling
     * {@code LOWER()} on an untyped SQL NULL, which PostgreSQL rejects.
     */
    @Query("""
            SELECT d FROM Doctor d
            WHERE (:specialtyId IS NULL OR d.specialty.id = :specialtyId)
              AND (:namePattern IS NULL OR LOWER(d.name) LIKE :namePattern)
              AND (:cnam IS NULL OR d.cnamConventionne = :cnam)
              AND (:governorate IS NULL OR d.governorate = :governorate)
            """)
    Page<Doctor> search(@Param("specialtyId") Long specialtyId,
                        @Param("namePattern") String namePattern,
                        @Param("cnam") Boolean cnam,
                        @Param("governorate") Governorate governorate,
                        Pageable pageable);
}
