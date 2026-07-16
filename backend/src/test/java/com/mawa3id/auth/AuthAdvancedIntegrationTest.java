package com.mawa3id.auth;

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

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the auth flows added on top of register/login: refresh-token
 * rotation and reuse detection, logout, email/phone verification and password reset.
 * The {@link CapturingChallengeSender} (test profile) exposes the delivered secret so
 * the verification/reset steps can be completed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAdvancedIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CapturingChallengeSender challengeSender;

    private static final AtomicInteger SEQ = new AtomicInteger();

    // --- refresh rotation ---------------------------------------------------

    @Test
    void refreshRotatesAndReplayOfOldTokenIsRejected() throws Exception {
        JsonNode reg = register();
        String firstRefresh = reg.get("refreshToken").asText();

        // First rotation succeeds and returns a different refresh token.
        MvcResult rotated = refresh(firstRefresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        String secondRefresh = objectMapper.readTree(rotated.getResponse().getContentAsString())
                .get("refreshToken").asText();
        org.assertj.core.api.Assertions.assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Replaying the now-rotated first token is treated as theft and rejected. (The
        // family-wide revocation this triggers is asserted in RefreshTokenServiceTest;
        // it cannot be re-read here because the bulk update bypasses the shared session.)
        refresh(firstRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRefreshTokenIsUnauthorized() throws Exception {
        refresh("not-a-real-token").andExpect(status().isUnauthorized());
    }

    // --- logout -------------------------------------------------------------

    @Test
    void logoutInvalidatesRefreshToken() throws Exception {
        String refreshToken = register().get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    // --- email verification -------------------------------------------------

    @Test
    void emailVerificationRoundTrip() throws Exception {
        JsonNode reg = register();
        String token = reg.get("token").asText();

        mockMvc.perform(post("/api/auth/verify/email/request").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String code = challengeSender.lastSecret();

        mockMvc.perform(post("/api/auth/verify/email/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        // Re-requesting once verified is a conflict.
        mockMvc.perform(post("/api/auth/verify/email/request").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void emailVerificationWithWrongCodeIsRejected() throws Exception {
        String token = register().get("token").asText();
        mockMvc.perform(post("/api/auth/verify/email/request").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/verify/email/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- phone verification -------------------------------------------------

    @Test
    void phoneVerificationRoundTrip() throws Exception {
        String token = register().get("token").asText();
        String phone = "+21261234" + String.format("%04d", SEQ.getAndIncrement());

        mockMvc.perform(post("/api/auth/verify/phone/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk());
        String code = challengeSender.lastSecret();

        mockMvc.perform(post("/api/auth/verify/phone/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void phoneVerificationRejectsInvalidNumber() throws Exception {
        String token = register().get("token").asText();
        mockMvc.perform(post("/api/auth/verify/phone/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- password reset -----------------------------------------------------

    @Test
    void forgotAndResetPasswordThenLoginWithNewPassword() throws Exception {
        String email = "reset" + SEQ.getAndIncrement() + "@example.com";
        register(email);

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        String resetToken = challengeSender.lastSecret();

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"newpass12345\"}"))
                .andExpect(status().isOk());

        // New password works; old one does not. (Reset also revokes existing sessions via
        // a bulk update; that query runs here and its effect is asserted in unit tests.)
        login(email, "newpass12345").andExpect(status().isOk());
        login(email, "password123").andExpect(status().isUnauthorized());
    }

    @Test
    void forgotForUnknownIdentifierStillReturns202() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"nobody-" + SEQ.getAndIncrement() + "@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetWithBadTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"garbage\",\"newPassword\":\"newpass12345\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ------------------------------------------------------------

    private JsonNode register() throws Exception {
        return register("user" + SEQ.getAndIncrement() + "@example.com");
    }

    private JsonNode register(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"Test User","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String identifier, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"));
    }
}
