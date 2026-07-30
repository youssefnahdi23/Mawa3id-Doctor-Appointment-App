package com.mawa3id.feedback;

import com.mawa3id.feedback.dto.FeedbackRequest;
import com.mawa3id.feedback.dto.FeedbackResponse;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@AuthenticationPrincipal AppUserDetails principal,
                                   @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submit(principal.getUserId(), request);
    }
}
