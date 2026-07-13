package com.mawa3id.review;

import com.mawa3id.review.dto.ReviewRequest;
import com.mawa3id.review.dto.ReviewResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/appointments/{appointmentId}/review")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal AppUserDetails principal,
                                 @PathVariable Long appointmentId,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(principal.getUserId(), appointmentId, request);
    }

    @PutMapping("/api/appointments/{appointmentId}/review")
    @PreAuthorize("hasRole('PATIENT')")
    public ReviewResponse update(@AuthenticationPrincipal AppUserDetails principal,
                                 @PathVariable Long appointmentId,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(principal.getUserId(), appointmentId, request);
    }

    @GetMapping("/api/appointments/{appointmentId}/review")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ReviewResponse get(@AuthenticationPrincipal AppUserDetails principal,
                              @PathVariable Long appointmentId) {
        return reviewService.get(principal.getUserId(), appointmentId);
    }

    @GetMapping("/api/doctors/{doctorId}/reviews")
    public Page<ReviewResponse> forDoctor(@PathVariable Long doctorId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        // Ordering is defined by the repository method (…OrderByCreatedAtDesc).
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return reviewService.listForDoctor(doctorId, pageable);
    }

    @GetMapping("/api/reviews/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ReviewResponse> mine(@AuthenticationPrincipal AppUserDetails principal) {
        return reviewService.listForPatient(principal.getUserId());
    }
}
