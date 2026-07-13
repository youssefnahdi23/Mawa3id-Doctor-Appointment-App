package com.mawa3id.record;

import com.mawa3id.record.dto.MedicalRecordResponse;
import com.mawa3id.record.dto.RecordRequest;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    public MedicalRecordController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/api/appointments/{appointmentId}/record")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRecordResponse create(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable Long appointmentId,
                                        @Valid @RequestBody RecordRequest request) {
        return recordService.create(principal.getUserId(), appointmentId, request);
    }

    @PutMapping("/api/appointments/{appointmentId}/record")
    @PreAuthorize("hasRole('DOCTOR')")
    public MedicalRecordResponse update(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable Long appointmentId,
                                        @Valid @RequestBody RecordRequest request) {
        return recordService.update(principal.getUserId(), appointmentId, request);
    }

    @GetMapping("/api/appointments/{appointmentId}/record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public MedicalRecordResponse get(@AuthenticationPrincipal AppUserDetails principal,
                                     @PathVariable Long appointmentId) {
        return recordService.get(principal.getUserId(), appointmentId);
    }

    @GetMapping("/api/records/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<MedicalRecordResponse> mine(@AuthenticationPrincipal AppUserDetails principal) {
        return recordService.listForPatient(principal.getUserId());
    }
}
