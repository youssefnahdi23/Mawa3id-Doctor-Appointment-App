package com.mawa3id.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void authenticatedUserCanSubmitFeedback() throws Exception {
        String token = registerPatient("fb1@example.com");

        String body = """
                {"category":"SUGGESTION","message":"Please add appointment reminders",
                 "appVersion":"1.0.0","platform":"ANDROID"}
                """;

        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("SUGGESTION"))
                .andExpect(jsonPath("$.message").value("Please add appointment reminders"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(feedbackRepository.count()).isEqualTo(1);
        Feedback stored = feedbackRepository.findAll().get(0);
        assertThat(stored.getCategory()).isEqualTo(FeedbackCategory.SUGGESTION);
        assertThat(stored.getPlatform()).isEqualTo("ANDROID");
    }

    @Test
    void anonymousCannotSubmitFeedback() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BUG\",\"message\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankMessageIsBadRequest() throws Exception {
        String token = registerPatient("fb2@example.com");
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BUG\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").isNotEmpty());
    }

    @Test
    void missingCategoryIsBadRequest() throws Exception {
        String token = registerPatient("fb3@example.com");
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"no category\"}"))
                .andExpect(status().isBadRequest());
    }

    private String registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"P","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
