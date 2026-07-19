package com.mawa3id.donation;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DonationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anonymousDonationReturnsPendingWithCheckoutUrl() throws Exception {
        donate(null, 500, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.currency").value("usd"))
                .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.startsWith(
                        StubPaymentGateway.URL_PREFIX)));
    }

    @Test
    void authenticatedDonationIsAttributedAndListed() throws Exception {
        String token = registerPatient("donor-listed@example.com");

        donate(token, 1500, "eur")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber());

        mockMvc.perform(get("/api/donations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].currency").value("eur"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].checkoutUrl").doesNotExist());
    }

    @Test
    void belowMinimumAmountIsRejected() throws Exception {
        donate(null, 50, null).andExpect(status().isBadRequest());
    }

    @Test
    void nonPositiveAmountFailsValidation() throws Exception {
        donate(null, 0, null).andExpect(status().isBadRequest());
    }

    @Test
    void webhookMarksDonationSucceeded() throws Exception {
        String token = registerPatient("donor-webhook@example.com");
        MvcResult created = donate(token, 2000, null).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String sessionId = StubPaymentGateway.SESSION_PREFIX + id;

        String event = """
                {"type":"checkout.session.completed","sessionId":"%s","paymentRef":"pi_test_1"}
                """.formatted(sessionId);
        mockMvc.perform(post("/api/donations/webhook")
                        .header("Stripe-Signature", "test-sig")
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/donations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
    }

    @Test
    void donationHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/donations/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void configExposesAllConfiguredMethods() throws Exception {
        mockMvc.perform(get("/api/donations/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardEnabled").value(true))
                .andExpect(jsonPath("$.minAmountMinor").value(100))
                .andExpect(jsonPath("$.patreonUrl").value("https://patreon.com/mawa3id-test"))
                .andExpect(jsonPath("$.konnectEnabled").value(true))
                .andExpect(jsonPath("$.konnectCurrency").value("tnd"))
                .andExpect(jsonPath("$.ribAccountHolder").value("Mawa3id Association"))
                .andExpect(jsonPath("$.ribBankName").value("Test Bank"))
                .andExpect(jsonPath("$.ribNumber").value("08 001 0000123456789 12"));
    }

    @Test
    void konnectDonationReturnsPayUrlAndWebhookCompletesIt() throws Exception {
        String token = registerPatient("donor-konnect@example.com");

        MvcResult created = mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":5000,\"provider\":\"KONNECT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("KONNECT"))
                .andExpect(jsonPath("$.currency").value("tnd"))
                .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.startsWith(
                        StubKonnectGateway.URL_PREFIX)))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String ref = StubKonnectGateway.REF_PREFIX + id;

        // Konnect calls back with GET ?payment_ref=; the stub reports COMPLETED.
        mockMvc.perform(get("/api/donations/konnect/webhook").param("payment_ref", ref))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/donations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
    }

    @Test
    void konnectWebhookWithUnknownOrMissingRefIsAcknowledged() throws Exception {
        mockMvc.perform(get("/api/donations/konnect/webhook").param("payment_ref", "nope"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/donations/konnect/webhook"))
                .andExpect(status().isOk());
    }

    @Test
    void ribDonationIsRecordedPendingWithoutCheckoutUrl() throws Exception {
        String token = registerPatient("donor-rib@example.com");

        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":20000,\"provider\":\"RIB\",\"message\":\"transfer ref 123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("RIB"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl").doesNotExist());
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.ResultActions donate(String token, long amount, String currency)
            throws Exception {
        String body = currency == null
                ? "{\"amountMinor\":%d}".formatted(amount)
                : "{\"amountMinor\":%d,\"currency\":\"%s\"}".formatted(amount, currency);
        var request = post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private String registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"Donor","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
