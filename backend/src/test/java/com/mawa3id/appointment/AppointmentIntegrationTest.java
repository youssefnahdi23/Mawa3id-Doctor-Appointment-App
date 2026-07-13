package com.mawa3id.appointment;

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
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long specialtyId;
    private final LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private final String slotNine = LocalDateTime.of(nextMonday, java.time.LocalTime.of(9, 0)).toString();
    private final String slotNineThirty = LocalDateTime.of(nextMonday, java.time.LocalTime.of(9, 30)).toString();

    @BeforeEach
    void seed() {
        specialtyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
    }

    @Test
    void patientAppointmentListIsPaginated() throws Exception {
        Doctor doctor = registerDoctor("page-doc@example.com", true);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("page-pat@example.com");

        book(patient, doctor.id(), slotNine).andExpect(status().isCreated());
        book(patient, doctor.id(), slotNineThirty).andExpect(status().isCreated());

        // size=1 returns a single element but reports the true total.
        mockMvc.perform(get("/api/appointments/me").param("size", "1")
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.size").value(1));
    }

    @Test
    void manualDoctorFlowFromBookingToAcceptance() throws Exception {
        Doctor doctor = registerDoctor("manual-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("book-pat@example.com");

        // Book -> PENDING (manual acceptance)
        MvcResult booked = book(patient, doctor.id(), slotNine)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long appointmentId = objectMapper.readTree(booked.getResponse().getContentAsString()).get("id").asLong();

        // Doctor sees it in the queue
        mockMvc.perform(get("/api/appointments").header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        // Doctor accepts
        mockMvc.perform(put("/api/appointments/{id}/accept", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Patient sees the accepted appointment
        mockMvc.perform(get("/api/appointments/me").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"));
    }

    @Test
    void autoDoctorAcceptsOnBooking() throws Exception {
        Doctor doctor = registerDoctor("auto-doc@example.com", true);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("auto-pat@example.com");

        book(patient, doctor.id(), slotNine)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void doubleBookingSameSlotIsConflict() throws Exception {
        Doctor doctor = registerDoctor("dbl-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("dbl-pat@example.com");

        book(patient, doctor.id(), slotNine).andExpect(status().isCreated());
        book(patient, doctor.id(), slotNine).andExpect(status().isConflict());
    }

    @Test
    void bookingTimeOutsideAvailabilityIsBadRequest() throws Exception {
        Doctor doctor = registerDoctor("out-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("out-pat@example.com");

        // 08:00 is before the 09:00–12:00 window.
        String eight = LocalDateTime.of(nextMonday, java.time.LocalTime.of(8, 0)).toString();
        book(patient, doctor.id(), eight).andExpect(status().isBadRequest());
    }

    @Test
    void doctorCannotBookAndPatientCannotAccept() throws Exception {
        Doctor doctor = registerDoctor("role-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("role-pat@example.com");

        // Doctor cannot book (PATIENT-only)
        book(doctor.token(), doctor.id(), slotNine).andExpect(status().isForbidden());

        // Patient books, then cannot accept (DOCTOR-only)
        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(booked.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(put("/api/appointments/{id}/accept", id)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelledSlotBecomesBookableAgain() throws Exception {
        Doctor doctor = registerDoctor("cancel-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("cancel-pat@example.com");

        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(booked.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/appointments/{id}/cancel", id)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Same slot can be booked again after cancellation.
        book(patient, doctor.id(), slotNine).andExpect(status().isCreated());
    }

    @Test
    void doctorCompletesAcceptedAppointment() throws Exception {
        Doctor doctor = registerDoctor("complete-doc@example.com", true);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("complete-pat@example.com");

        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(booked.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/appointments/{id}/complete", id)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Completing again (now COMPLETED, not ACCEPTED) is a conflict.
        mockMvc.perform(put("/api/appointments/{id}/complete", id)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void completingPendingAppointmentIsConflict() throws Exception {
        Doctor doctor = registerDoctor("complete-pend-doc@example.com", false);
        setMondayAvailability(doctor.token());
        String patient = registerPatient("complete-pend-pat@example.com");

        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(booked.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/appointments/{id}/complete", id)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isConflict());
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.ResultActions book(String token, long doctorId, String start)
            throws Exception {
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

    /** Test holder for a registered doctor's token and user id. */
    private record Doctor(String token, long id) {
    }
}
