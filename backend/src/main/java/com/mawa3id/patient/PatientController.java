package com.mawa3id.patient;

import com.mawa3id.patient.dto.PatientResponse;
import com.mawa3id.patient.dto.PatientUpdateRequest;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return PatientResponse.from(patientService.getByUserId(principal.getUserId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientResponse update(@AuthenticationPrincipal AppUserDetails principal,
                                  @Valid @RequestBody PatientUpdateRequest request) {
        return PatientResponse.from(patientService.update(principal.getUserId(), request));
    }
}
