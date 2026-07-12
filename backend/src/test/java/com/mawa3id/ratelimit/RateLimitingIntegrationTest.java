package com.mawa3id.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the auth rate limiter. This test owns its own Spring context (a low
 * capacity is set via properties) because the shared test profile disables rate
 * limiting so the rest of the suite isn't throttled.
 */
@SpringBootTest(properties = {
        "mawa3id.ratelimit.enabled=true",
        "mawa3id.ratelimit.auth.capacity=3",
        "mawa3id.ratelimit.auth.window-seconds=60"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String LOGIN_BODY = """
            {"email":"nobody@example.com","password":"wrongpass"}
            """;

    @Test
    void loginIsThrottledAfterCapacityIsExceeded() throws Exception {
        // Capacity is 3: the first three attempts fail auth (401) but are allowed
        // through; the fourth is rejected by the limiter with 429.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void publicNonAuthEndpointIsNotThrottled() throws Exception {
        // Well above the capacity of 3: the specialties listing is exempt from the limiter.
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/specialties")).andExpect(status().isOk());
        }
    }
}
