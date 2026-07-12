package com.mawa3id.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.specialty.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
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
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long specialtyId;

    @BeforeEach
    void seedSpecialty() {
        specialtyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
    }

    @Test
    void patientRegisterLoginAndMeFlow() throws Exception {
        String registerBody = """
                {"email":"alice@example.com","password":"password123",
                 "fullName":"Alice Doe","dateOfBirth":"1990-05-20"}
                """;

        MvcResult registered = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = tokenFrom(registered);

        // /me requires authentication -> 401 without token
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // With token -> 200 and correct identity
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"));

        // Patient profile has an auto-generated code
        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCode").value(org.hamcrest.Matchers.startsWith("MW-")));

        // Login returns a fresh token
        String loginBody = """
                {"email":"alice@example.com","password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        String registerBody = """
                {"email":"bob@example.com","password":"password123",
                 "fullName":"Bob Roe","dateOfBirth":"1985-01-01"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String body = """
                {"email":"dup@example.com","password":"password123",
                 "fullName":"Dup User","dateOfBirth":"1990-01-01"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void validationRejectsShortPassword() throws Exception {
        String body = """
                {"email":"short@example.com","password":"123",
                 "fullName":"Short Pw","dateOfBirth":"1990-01-01"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void doctorRegisterAndMeShowsDoctorRole() throws Exception {
        String body = """
                {"email":"doc@example.com","password":"password123","name":"Dr Who",
                 "specialtyId":%d,"cabinetAddress":"Clinic","workingHours":"9-17","phone":"+212"}
                """.formatted(specialtyId);

        MvcResult registered = mockMvc.perform(post("/api/auth/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andReturn();

        String token = tokenFrom(registered);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void doctorRegisterWithInvalidSpecialtyIs404() throws Exception {
        String body = """
                {"email":"nospec@example.com","password":"password123","name":"Dr Ghost",
                 "specialtyId":999999}
                """;
        mockMvc.perform(post("/api/auth/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void emailIsNormalizedToLowercase() throws Exception {
        String body = """
                {"email":"Alice@Example.COM","password":"password123",
                 "fullName":"Alice","dateOfBirth":"1990-05-20"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // Login with a different casing still succeeds
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidEmailIsBadRequest() throws Exception {
        String body = """
                {"email":"not-an-email","password":"password123",
                 "fullName":"X","dateOfBirth":"1990-01-01"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void missingRequiredFieldIsBadRequest() throws Exception {
        // fullName omitted
        String body = """
                {"email":"nofull@example.com","password":"password123","dateOfBirth":"1990-01-01"}
                """;
        mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").isNotEmpty());
    }

    @Test
    void garbageBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFrom(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
