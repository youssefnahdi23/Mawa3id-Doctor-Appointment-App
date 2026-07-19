package com.mawa3id.notification;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private static final String TOKEN = "fcm-token-xyz";

    @Test
    void registerPersistsTokenForTheCaller() throws Exception {
        Account account = registerPatient("dev-reg@example.com");

        registerDevice(account.token(), TOKEN, "ANDROID").andExpect(status().isNoContent());

        DeviceToken stored = deviceTokenRepository.findByToken(TOKEN).orElseThrow();
        assertThat(stored.getRecipient().getId()).isEqualTo(account.userId());
        assertThat(stored.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
    }

    @Test
    void registeringSameTokenTwiceUpsertsRatherThanDuplicates() throws Exception {
        Account account = registerPatient("dev-upsert@example.com");

        registerDevice(account.token(), TOKEN, "ANDROID").andExpect(status().isNoContent());
        registerDevice(account.token(), TOKEN, "IOS").andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByRecipientId(account.userId())).hasSize(1);
        assertThat(deviceTokenRepository.findByToken(TOKEN).orElseThrow().getPlatform())
                .isEqualTo(DevicePlatform.IOS);
    }

    @Test
    void registeringAnExistingTokenReassignsItToTheNewUser() throws Exception {
        Account first = registerPatient("dev-first@example.com");
        Account second = registerPatient("dev-second@example.com");

        registerDevice(first.token(), TOKEN, "ANDROID").andExpect(status().isNoContent());
        registerDevice(second.token(), TOKEN, "ANDROID").andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByRecipientId(first.userId())).isEmpty();
        assertThat(deviceTokenRepository.findByToken(TOKEN).orElseThrow().getRecipient().getId())
                .isEqualTo(second.userId());
    }

    @Test
    void unregisterRemovesTheToken() throws Exception {
        Account account = registerPatient("dev-del@example.com");
        registerDevice(account.token(), TOKEN, "ANDROID").andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/notifications/devices")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByToken(TOKEN)).isEmpty();
    }

    @Test
    void unregisterIsIdempotentForAnUnknownToken() throws Exception {
        Account account = registerPatient("dev-noop@example.com");

        mockMvc.perform(delete("/api/notifications/devices")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"never-registered\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void blankTokenIsRejected() throws Exception {
        Account account = registerPatient("dev-blank@example.com");

        registerDevice(account.token(), "", "ANDROID").andExpect(status().isBadRequest());
    }

    @Test
    void registrationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.ResultActions registerDevice(
            String token, String value, String platform) throws Exception {
        String body = """
                {"token":"%s","platform":"%s"}
                """.formatted(value, platform);
        return mockMvc.perform(post("/api/notifications/devices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Account registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"Pat","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Account(node.get("token").asText(), node.get("userId").asLong());
    }

    private record Account(String token, long userId) {
    }
}
