package com.mawa3id.schedule;

import com.mawa3id.schedule.dto.AvailabilityRuleResponse;
import com.mawa3id.schedule.dto.AvailabilityUpdateRequest;
import com.mawa3id.schedule.dto.SlotResponse;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class AvailabilityController {

    private final ScheduleService scheduleService;

    public AvailabilityController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/{id}/availability")
    public List<AvailabilityRuleResponse> availability(@PathVariable Long id) {
        return scheduleService.getAvailability(id);
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<AvailabilityRuleResponse> updateAvailability(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody AvailabilityUpdateRequest request) {
        return scheduleService.replaceAvailability(principal.getUserId(), request.rules());
    }

    @GetMapping("/{id}/slots")
    public List<SlotResponse> slots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleService.computeFreeSlots(id, from, to);
    }
}
