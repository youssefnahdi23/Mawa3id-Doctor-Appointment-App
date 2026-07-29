package com.mawa3id.doctor;

import com.mawa3id.doctor.dto.DoctorResponse;
import com.mawa3id.doctor.dto.DoctorSummary;
import com.mawa3id.doctor.dto.DoctorUpdateRequest;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private static final int MAX_PAGE_SIZE = 100;

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public Page<DoctorSummary> list(
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean cnam,
            @RequestParam(required = false) Governorate governorate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("name"));
        return doctorService.search(specialtyId, q, cnam, governorate, pageable).map(DoctorSummary::from);
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id) {
        return DoctorResponse.from(doctorService.getByUserId(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return DoctorResponse.from(doctorService.getByUserId(principal.getUserId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorResponse update(@AuthenticationPrincipal AppUserDetails principal,
                                 @Valid @RequestBody DoctorUpdateRequest request) {
        return DoctorResponse.from(doctorService.update(principal.getUserId(), request));
    }
}
