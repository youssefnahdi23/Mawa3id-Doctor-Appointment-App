package com.mawa3id.record;

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
class MedicalRecordIntegrationTest {

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
    void doctorRecordsVisitAndPatientReadsIt() throws Exception {
        Doctor doctor = registerDoctor("rec-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("rec-pat@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        // Doctor attaches a record; the ACCEPTED appointment is auto-completed.
        MvcResult recorded = record(doctor.token(), appointmentId, "Flu", LocalDate.now().plusDays(7))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosis").value("Flu"))
                .andExpect(jsonPath("$.appointmentId").value((int) appointmentId))
                .andReturn();
        assertNotBlank(recorded);

        // Appointment is now COMPLETED.
        mockMvc.perform(get("/api/appointments/me").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        // Both parties can read the record.
        mockMvc.perform(get("/api/appointments/{id}/record", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Flu"));
        mockMvc.perform(get("/api/appointments/{id}/record", appointmentId)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Pat"));

        // Patient visit history lists the record.
        mockMvc.perform(get("/api/records/me").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].diagnosis").value("Flu"));
    }

    @Test
    void updateRewritesTheRecord() throws Exception {
        Doctor doctor = registerDoctor("upd-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("upd-pat@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        record(doctor.token(), appointmentId, "Flu", null).andExpect(status().isCreated());

        String body = """
                {"diagnosis":"Bronchitis","notes":"Antibiotics","prescription":"Amoxicillin"}
                """;
        mockMvc.perform(put("/api/appointments/{id}/record", appointmentId)
                        .header("Authorization", "Bearer " + doctor.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Bronchitis"))
                .andExpect(jsonPath("$.prescription").value("Amoxicillin"));
    }

    @Test
    void secondRecordForSameAppointmentIsConflict() throws Exception {
        Doctor doctor = registerDoctor("dup-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("dup-pat@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        record(doctor.token(), appointmentId, "Flu", null).andExpect(status().isCreated());
        record(doctor.token(), appointmentId, "Flu again", null).andExpect(status().isConflict());
    }

    @Test
    void recordingBeforeAppointmentAcceptedIsConflict() throws Exception {
        // Manual doctor -> booking is PENDING, so a record cannot be attached yet.
        Doctor doctor = registerDoctor("pend-doc@example.com");
        makeManual(doctor.token());
        setMondayAvailability(doctor.token());
        String patient = registerPatient("pend-pat@example.com");

        MvcResult booked = book(patient, doctor.id(), slotNine).andExpect(status().isCreated()).andReturn();
        long appointmentId = idOf(booked);

        record(doctor.token(), appointmentId, "Flu", null).andExpect(status().isConflict());
    }

    @Test
    void readingRecordBeforeItExistsIsNotFound() throws Exception {
        Doctor doctor = registerDoctor("nf-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("nf-pat@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        mockMvc.perform(get("/api/appointments/{id}/record", appointmentId)
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherPatientCannotReadRecord() throws Exception {
        Doctor doctor = registerDoctor("auth-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("auth-pat@example.com");
        String otherPatient = registerPatient("auth-other@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        record(doctor.token(), appointmentId, "Flu", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/appointments/{id}/record", appointmentId)
                        .header("Authorization", "Bearer " + otherPatient))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotCreateRecord() throws Exception {
        Doctor doctor = registerDoctor("role-doc@example.com");
        setMondayAvailability(doctor.token());
        String patient = registerPatient("role-pat@example.com");
        long appointmentId = bookAcceptedSlot(patient, doctor.id());

        record(patient, appointmentId, "Flu", null).andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private long bookAcceptedSlot(String patientToken, long doctorId) throws Exception {
        // Doctors registered here use AUTO acceptance, so bookings are ACCEPTED immediately.
        MvcResult booked = book(patientToken, doctorId, slotNine)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();
        return idOf(booked);
    }

    private long idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void assertNotBlank(MvcResult result) throws Exception {
        objectMapper.readTree(result.getResponse().getContentAsString()).get("createdAt").asText();
    }

    private ResultActions record(String token, long appointmentId, String diagnosis, LocalDate followUp)
            throws Exception {
        String followUpField = followUp == null ? "" : ",\"followUpDate\":\"" + followUp + "\"";
        String body = """
                {"diagnosis":"%s","notes":"Rest and fluids"%s}
                """.formatted(diagnosis, followUpField);
        return mockMvc.perform(post("/api/appointments/{id}/record", appointmentId)
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

    private void makeManual(String doctorToken) throws Exception {
        String update = """
                {"name":"Dr Book","specialtyId":%d,"acceptanceMode":"MANUAL"}
                """.formatted(specialtyId);
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
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

        // Default doctors here use AUTO acceptance for a one-call ACCEPTED booking.
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
