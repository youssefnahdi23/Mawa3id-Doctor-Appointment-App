package com.mawa3id.schedule;

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
import java.time.temporal.TemporalAdjusters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvailabilityIntegrationTest {

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
    void doctorSetsAvailabilityThenItIsPubliclyReadable() throws Exception {
        String token = registerDoctor("avail@example.com");
        long doctorId = userIdFor(token);

        String body = """
                {"rules":[{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"}]}
                """;
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));

        // Public read, no auth.
        mockMvc.perform(get("/api/doctors/{id}/availability", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"));
    }

    @Test
    void slotsAreComputedFromAvailability() throws Exception {
        String token = registerDoctor("slots@example.com");
        long doctorId = userIdFor(token);

        String body = """
                {"rules":[{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"}]}
                """;
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        LocalDate monday = LocalDate.now().plusDays(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        // 09:00–12:00 at the default 30-minute slot => 6 slots.
        mockMvc.perform(get("/api/doctors/{id}/slots", doctorId)
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void rdvOnlyFlagRoundTripsAndDoesNotChangeSlots() throws Exception {
        String token = registerDoctor("rdv-avail@example.com");
        long doctorId = userIdFor(token);

        // Monday is RDVs-only; Tuesday omits the flag (defaults to walk-in / false).
        String body = """
                {"rules":[
                  {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00","rdvOnly":true},
                  {"dayOfWeek":"TUESDAY","startTime":"09:00","endTime":"12:00"}
                ]}
                """;
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].rdvOnly").value(true))
                .andExpect(jsonPath("$[1].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$[1].rdvOnly").value(false));

        // Public read exposes the flag.
        mockMvc.perform(get("/api/doctors/{id}/availability", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rdvOnly").value(true));

        // The RDV-only Monday still generates the usual 6 bookable slots.
        LocalDate monday = LocalDate.now().plusDays(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        mockMvc.perform(get("/api/doctors/{id}/slots", doctorId)
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void patientCannotSetAvailability() throws Exception {
        String patientToken = registerPatient("pat-avail@example.com");
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rules\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endBeforeStartIsBadRequest() throws Exception {
        String token = registerDoctor("badrule@example.com");
        String body = """
                {"rules":[{"dayOfWeek":"MONDAY","startTime":"12:00","endTime":"09:00"}]}
                """;
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    private String registerDoctor(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","name":"Dr Avail","specialtyId":%d}
                """.formatted(email, specialtyId);
        MvcResult result = mockMvc.perform(post("/api/auth/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return tokenFrom(result);
    }

    private String registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"P","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return tokenFrom(result);
    }

    private String tokenFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long userIdFor(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(me.getResponse().getContentAsString()).get("userId").asLong();
    }
}
