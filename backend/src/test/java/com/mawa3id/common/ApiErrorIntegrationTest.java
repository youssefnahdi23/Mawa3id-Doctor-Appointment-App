package com.mawa3id.common;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks that malformed requests produce consistent {@link ApiError}
 * responses with the correct 4xx status, rather than falling through to a generic 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void malformedJsonBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{ not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void nonNumericPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/doctors/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unsupportedHttpMethodIsMethodNotAllowed() throws Exception {
        // /api/auth/login is permitAll for every method but only maps POST.
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownRouteForAuthenticatedUserIsNotFound() throws Exception {
        String token = registerPatient();

        mockMvc.perform(get("/api/definitely-not-a-real-route").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String registerPatient() throws Exception {
        String body = """
                {"email":"err@example.com","password":"password123",
                 "fullName":"Err User","dateOfBirth":"1990-01-01"}
                """;
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
