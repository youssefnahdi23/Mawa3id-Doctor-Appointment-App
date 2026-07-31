package com.mawa3id.feedback;

import com.mawa3id.feedback.dto.FeedbackRequest;
import com.mawa3id.feedback.dto.FeedbackResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public FeedbackResponse submit(Long userId, FeedbackRequest request) {
        Feedback saved = feedbackRepository.save(new Feedback(
                userId,
                request.category(),
                request.message().trim(),
                blankToNull(request.appVersion()),
                blankToNull(request.platform())));
        return FeedbackResponse.from(saved);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
