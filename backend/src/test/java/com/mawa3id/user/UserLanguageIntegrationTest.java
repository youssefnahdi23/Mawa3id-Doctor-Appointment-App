package com.mawa3id.user;

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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserLanguageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long specialtyId;

    @BeforeEach
    void seed() {
        specialtyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
    }

    @Test
    void anonymousCannotSetLanguage() throws Exception {
        mockMvc.perform(put("/api/users/me/language")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"ar\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedLanguageIsRejected() throws Exception {
        String token = registerPatient("lang-bad@example.com");
        mockMvc.perform(put("/api/users/me/language")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"es\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notificationIsLocalizedToTheRecipientLanguage() throws Exception {
        String[] doctor = registerDoctor("lang-doc@example.com"); // {token, id}
        // Doctor prefers Arabic; the booking notification (addressed to the doctor) must be Arabic.
        mockMvc.perform(put("/api/users/me/language")
                        .header("Authorization", "Bearer " + doctor[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"ar\"}"))
                .andExpect(status().isNoContent());
        setMondayAvailability(doctor[0]);

        String patient = registerPatient("lang-pat@example.com");
        String slot = LocalDateTime.of(
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)), LocalTime.of(9, 0)).toString();
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":%s,\"startTime\":\"%s\",\"reason\":\"x\"}".formatted(doctor[1], slot)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + doctor[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("APPOINTMENT_BOOKED"))
                .andExpect(jsonPath("$.content[0].message", containsString("موعد")));
    }

    private void setMondayAvailability(String doctorToken) throws Exception {
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rules\":[{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}]}"))
                .andExpect(status().isOk());
    }

    private String[] registerDoctor(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","name":"Dr Book","specialtyId":%d}
                """.formatted(email, specialtyId);
        MvcResult result = mockMvc.perform(post("/api/auth/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new String[]{node.get("token").asText(), node.get("userId").asText()};
    }

    private String registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"Pat","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
