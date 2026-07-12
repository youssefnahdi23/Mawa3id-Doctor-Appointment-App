package com.mawa3id.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the security filter chain: the {@link JwtAuthenticationFilter}
 * negative branches (missing / non-Bearer Authorization header) and the public-vs-protected
 * route matrix configured in SecurityConfig. Happy-path authentication is covered by the
 * feature integration tests; this focuses on the paths those do not reach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointWithoutAuthorizationHeaderIsUnauthorized() {
        assertUnauthorizedMe(get("/api/auth/me"));
    }

    @Test
    void protectedEndpointWithNonBearerSchemeIsUnauthorized() throws Exception {
        // Filter only acts on a "Bearer " prefix; anything else is ignored and the
        // request reaches the entry point unauthenticated.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithBearerButBlankTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicSpecialtiesEndpointReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/specialties")).andExpect(status().isOk());
    }

    @Test
    void publicDoctorsListReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/doctors")).andExpect(status().isOk());
    }

    private void assertUnauthorizedMe(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        try {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
