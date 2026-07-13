package com.mawa3id.appointment;

import com.mawa3id.appointment.dto.AppointmentResponse;
import com.mawa3id.appointment.dto.BookAppointmentRequest;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@AuthenticationPrincipal AppUserDetails principal,
                                    @Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.book(principal.getUserId(), request.doctorId(),
                request.startTime(), request.reason());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public Page<AppointmentResponse> mine(@AuthenticationPrincipal AppUserDetails principal,
                                          @RequestParam(required = false) AppointmentStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return appointmentService.listForPatient(principal.getUserId(), status, pageable(page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public Page<AppointmentResponse> queue(@AuthenticationPrincipal AppUserDetails principal,
                                           @RequestParam(required = false) AppointmentStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return appointmentService.listForDoctor(principal.getUserId(), status, pageable(page, size));
    }

    private static Pageable pageable(int page, int size) {
        // Ordering is defined by the repository methods (…OrderByStartTime…).
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse accept(@AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id) {
        return appointmentService.accept(principal.getUserId(), id);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse reject(@AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id) {
        return appointmentService.reject(principal.getUserId(), id);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse complete(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable Long id) {
        return appointmentService.complete(principal.getUserId(), id);
    }

    @PutMapping("/{id}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id) {
        return appointmentService.cancel(principal.getUserId(), id);
    }
}
