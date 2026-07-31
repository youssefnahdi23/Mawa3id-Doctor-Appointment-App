package com.mawa3id.feedback;

import com.mawa3id.feedback.dto.FeedbackRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Captor
    private ArgumentCaptor<Feedback> feedbackCaptor;

    @Test
    void submitPersistsTrimmedFeedbackForUser() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.submit(7L, new FeedbackRequest(
                FeedbackCategory.BUG, "  slot picker crashes  ", "1.0.0", "ANDROID"));

        org.mockito.Mockito.verify(feedbackRepository).save(feedbackCaptor.capture());
        Feedback saved = feedbackCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getCategory()).isEqualTo(FeedbackCategory.BUG);
        assertThat(saved.getMessage()).isEqualTo("slot picker crashes");
        assertThat(saved.getAppVersion()).isEqualTo("1.0.0");
        assertThat(saved.getPlatform()).isEqualTo("ANDROID");
    }

    @Test
    void submitNormalisesBlankMetadataToNull() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.submit(1L, new FeedbackRequest(
                FeedbackCategory.SUGGESTION, "add dark mode", "  ", ""));

        org.mockito.Mockito.verify(feedbackRepository).save(feedbackCaptor.capture());
        Feedback saved = feedbackCaptor.getValue();
        assertThat(saved.getAppVersion()).isNull();
        assertThat(saved.getPlatform()).isNull();
    }
}
