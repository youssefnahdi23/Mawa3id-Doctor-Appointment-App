package com.mawa3id.notification;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long specialtyId;
    private final LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private final String slotNine = LocalDateTime.of(nextMonday, java.time.LocalTime.of(9, 0)).toString();

    @BeforeEach
    void seed() {
        specialtyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
    }

    @Test
    void lifecycleTransitionsNotifyTheAffectedParty() throws Exception {
        Doctor doctor = registerDoctor("notif-doc@example.com", false); // MANUAL
        setMondayAvailability(doctor.token());
        String patient = registerPatient("notif-pat@example.com");

        // Booking (PENDING) notifies the doctor.
        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long appointmentId = idOf(booked);
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("APPOINTMENT_BOOKED"))
                .andExpect(jsonPath("$.content[0].appointmentId").value((int) appointmentId))
                .andExpect(jsonPath("$.content[0].read").value(false));

        // Accepting notifies the patient.
        mockMvc.perform(put("/api/appointments/{id}/accept", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("APPOINTMENT_ACCEPTED"));

        // Completing notifies the patient again.
        mockMvc.perform(put("/api/appointments/{id}/complete", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("APPOINTMENT_COMPLETED"));
    }

    @Test
    void cancellingNotifiesTheOtherParty() throws Exception {
        Doctor doctor = registerDoctor("cancel-doc@example.com", true); // AUTO -> ACCEPTED
        setMondayAvailability(doctor.token());
        String patient = registerPatient("cancel-pat@example.com");

        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long appointmentId = idOf(booked);

        // Patient cancels -> the doctor is notified.
        mockMvc.perform(put("/api/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").param("unread", "true")
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'APPOINTMENT_CANCELLED')]").isNotEmpty());
    }

    @Test
    void unreadCountAndMarkReadFlow() throws Exception {
        Doctor doctor = registerDoctor("read-doc@example.com", true);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("read-pat@example.com");
        long appointmentId = idOf(book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn());

        // Doctor has one unread (APPOINTMENT_BOOKED).
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        long notificationId = firstNotificationId(doctor.token());
        mockMvc.perform(put("/api/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // Cancel to create another unread, then read-all clears it.
        mockMvc.perform(put("/api/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/notifications/read-all").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").isNumber());
    }

    @Test
    void cannotMarkAnotherUsersNotificationRead() throws Exception {
        Doctor doctor = registerDoctor("x-doc@example.com", true);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("x-pat@example.com");
        book(patient, doctor.id(), slotNine).andExpect(status().isCreated());

        long doctorNotificationId = firstNotificationId(doctor.token());
        mockMvc.perform(put("/api/notifications/{id}/read", doctorNotificationId)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private long firstNotificationId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();
    }

    private long idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions book(String token, long doctorId, String start) throws Exception {
        String body = """
                {"doctorId":%d,"startTime":"%s","reason":"checkup"}
                """.formatted(doctorId, start);
        return mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void setMondayAvailability(String doctorToken) throws Exception {
        String body = """
                {"rules":[{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"}]}
                """;
        mockMvc.perform(put("/api/doctors/me/availability")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private Doctor registerDoctor(String email, boolean auto) throws Exception {
        String body = """
                {"email":"%s","password":"password123","name":"Dr Book","specialtyId":%d}
                """.formatted(email, specialtyId);
        MvcResult result = mockMvc.perform(post("/api/auth/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();
        long id = node.get("userId").asLong();

        if (auto) {
            String update = """
                    {"name":"Dr Book","specialtyId":%d,"acceptanceMode":"AUTO"}
                    """.formatted(specialtyId);
            mockMvc.perform(put("/api/doctors/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(update))
                    .andExpect(status().isOk());
        }
        return new Doctor(token, id);
    }

    private String registerPatient(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","fullName":"Pat","dateOfBirth":"1990-01-01"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private record Doctor(String token, long id) {
    }
}
