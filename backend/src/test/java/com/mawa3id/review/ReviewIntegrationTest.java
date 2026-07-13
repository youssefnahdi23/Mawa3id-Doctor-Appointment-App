package com.mawa3id.review;

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
class ReviewIntegrationTest {

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
    void patientReviewsDoctorAndAggregateSurfaces() throws Exception {
        Doctor doctor = registerDoctor("rev-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("rev-pat@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        // Patient leaves a 5-star review.
        review(patient, appointmentId, 5, "Great doctor")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.appointmentId").value((int) appointmentId));

        // Doctor can read the review.
        mockMvc.perform(get("/api/appointments/{id}/review", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Great doctor"));

        // Public per-doctor reviews list.
        mockMvc.perform(get("/api/doctors/{id}/reviews", doctor.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5));

        // Aggregate surfaces on detail and browse.
        mockMvc.perform(get("/api/doctors/{id}", doctor.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingAverage").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1));
        mockMvc.perform(get("/api/doctors").param("specialtyId", specialtyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ratingAverage").value(5.0))
                .andExpect(jsonPath("$.content[0].ratingCount").value(1));

        // Patient sees the review in their own history.
        mockMvc.perform(get("/api/reviews/me").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void updatingReviewChangesDoctorAverage() throws Exception {
        Doctor doctor = registerDoctor("avg-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("avg-pat@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        review(patient, appointmentId, 4, "Good").andExpect(status().isCreated());
        mockMvc.perform(get("/api/doctors/{id}", doctor.id()))
                .andExpect(jsonPath("$.ratingAverage").value(4.0));

        // Update the rating; the doctor's average follows.
        review(patient, appointmentId, 2, "Reconsidered", put("/api/appointments/{id}/review", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2));
        mockMvc.perform(get("/api/doctors/{id}", doctor.id()))
                .andExpect(jsonPath("$.ratingAverage").value(2.0))
                .andExpect(jsonPath("$.ratingCount").value(1));
    }

    @Test
    void secondReviewForSameAppointmentIsConflict() throws Exception {
        Doctor doctor = registerDoctor("dup-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("dup-pat@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        review(patient, appointmentId, 5, "Great").andExpect(status().isCreated());
        review(patient, appointmentId, 3, "Again").andExpect(status().isConflict());
    }

    @Test
    void reviewingBeforeCompletionIsConflict() throws Exception {
        Doctor doctor = registerDoctor("early-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("early-pat@example.com");

        // Booked (ACCEPTED via AUTO) but not completed.
        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long appointmentId = idOf(booked);

        review(patient, appointmentId, 5, "Too soon").andExpect(status().isConflict());
    }

    @Test
    void anotherPatientCannotReviewSomeoneElsesAppointment() throws Exception {
        Doctor doctor = registerDoctor("auth-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("auth-pat@example.com");
        String otherPatient = registerPatient("auth-other@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        review(otherPatient, appointmentId, 1, "Not mine").andExpect(status().isForbidden());
    }

    @Test
    void doctorCannotCreateReview() throws Exception {
        Doctor doctor = registerDoctor("role-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("role-pat@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        review(doctor.token(), appointmentId, 5, "Self review").andExpect(status().isForbidden());
    }

    @Test
    void invalidRatingIsBadRequest() throws Exception {
        Doctor doctor = registerDoctor("bad-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("bad-pat@example.com");
        long appointmentId = completedAppointment(patient, doctor, slotNine);

        review(patient, appointmentId, 6, "Out of range").andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    /** Books an AUTO-accepted slot and completes it (as the doctor), returning the appointment id. */
    private long completedAppointment(String patientToken, Doctor doctor, String slot) throws Exception {
        MvcResult booked = book(patientToken, doctor.id(), slot)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();
        long appointmentId = idOf(booked);
        mockMvc.perform(put("/api/appointments/{id}/complete", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        return appointmentId;
    }

    private long idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions review(String token, long appointmentId, int rating, String comment) throws Exception {
        return review(token, appointmentId, rating, comment, post("/api/appointments/{id}/review", appointmentId));
    }

    private ResultActions review(String token, long appointmentId, int rating, String comment,
                                 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        String body = """
                {"rating":%d,"comment":"%s"}
                """.formatted(rating, comment);
        return mockMvc.perform(builder
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
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

    private Doctor registerDoctor(String email) throws Exception {
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

        // AUTO acceptance so a booking is ACCEPTED in one call.
        String update = """
                {"name":"Dr Book","specialtyId":%d,"acceptanceMode":"AUTO"}
                """.formatted(specialtyId);
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk());
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
