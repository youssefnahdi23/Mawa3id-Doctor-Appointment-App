package com.mawa3id.doctor;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DoctorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long cardiologyId;
    private Long dermatologyId;

    @BeforeEach
    void seedSpecialties() {
        cardiologyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
        dermatologyId = specialtyRepository.save(new Specialty("Dermatology", "Skin")).getId();
    }

    @Test
    void registerDoctorBrowseAndFilterBySpecialty() throws Exception {
        registerDoctor("cardio@example.com", "Dr Heart", cardiologyId);
        registerDoctor("derma@example.com", "Dr Skin", dermatologyId);

        // Public listing returns both
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        // Filter by specialty returns only the matching doctor
        mockMvc.perform(get("/api/doctors").param("specialtyId", cardiologyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dr Heart"))
                .andExpect(jsonPath("$.content[0].specialtyName").value("Cardiology"));

        // Name search
        mockMvc.perform(get("/api/doctors").param("q", "skin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dr Skin"));
    }

    @Test
    void doctorCanUpdateOwnProfileIncludingAcceptanceMode() throws Exception {
        String token = registerDoctor("update@example.com", "Dr Before", cardiologyId);

        String update = """
                {"name":"Dr After","specialtyId":%d,"cabinetAddress":"12 Main St",
                 "workingHours":"Mon-Fri 9-17","phone":"+21620000000",
                 "bio":"Experienced","acceptanceMode":"AUTO"}
                """.formatted(dermatologyId);

        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr After"))
                .andExpect(jsonPath("$.acceptanceMode").value("AUTO"))
                .andExpect(jsonPath("$.specialty.name").value("Dermatology"));
    }

    @Test
    void doctorCanUpdateAndFilterByCnamAndGovernorate() throws Exception {
        String token = registerDoctor("cnam@example.com", "Dr Sfax", cardiologyId);
        registerDoctor("other@example.com", "Dr Tunis", dermatologyId);

        String update = """
                {"name":"Dr Sfax","specialtyId":%d,"cnamConventionne":true,
                 "governorate":"SFAX","consultationFee":40,"languages":["ar","fr"]}
                """.formatted(cardiologyId);

        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnamConventionne").value(true))
                .andExpect(jsonPath("$.governorate").value("SFAX"))
                .andExpect(jsonPath("$.consultationFee").value(40))
                .andExpect(jsonPath("$.languages[0]").value("AR"))
                .andExpect(jsonPath("$.languages[1]").value("FR"));

        // Filter by CNAM returns only the conventionné doctor.
        mockMvc.perform(get("/api/doctors").param("cnam", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dr Sfax"))
                .andExpect(jsonPath("$.content[0].governorate").value("SFAX"))
                .andExpect(jsonPath("$.content[0].consultationFee").value(40))
                .andExpect(jsonPath("$.content[0].specialtyId").value(cardiologyId));

        // Filter by governorate returns only the SFAX doctor.
        mockMvc.perform(get("/api/doctors").param("governorate", "SFAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dr Sfax"));
    }

    @Test
    void updateWithUnsupportedLanguageIsBadRequest() throws Exception {
        String token = registerDoctor("badlang@example.com", "Dr Lang", cardiologyId);
        String update = """
                {"name":"Dr Lang","languages":["es"]}
                """;
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownGovernorateFilterIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/doctors").param("governorate", "ATLANTIS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patientCannotUpdateDoctorProfile() throws Exception {
        String patientToken = registerPatient("patient2@example.com");
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacker\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorDetailForMissingIdIs404() throws Exception {
        mockMvc.perform(get("/api/doctors/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void doctorCanFetchOwnProfileViaMe() throws Exception {
        String token = registerDoctor("me@example.com", "Dr Self", cardiologyId);
        mockMvc.perform(get("/api/doctors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr Self"))
                .andExpect(jsonPath("$.acceptanceMode").value("MANUAL"));
    }

    @Test
    void anonymousCannotFetchDoctorMe() throws Exception {
        // URL is permitAll but @PreAuthorize('hasRole DOCTOR') denies the anonymous user.
        mockMvc.perform(get("/api/doctors/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWithInvalidSpecialtyIs404() throws Exception {
        String token = registerDoctor("badspec@example.com", "Dr Bad", cardiologyId);
        String update = """
                {"name":"Dr Bad","specialtyId":999999}
                """;
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void doctorCanSetSlotDurationAndDefaultsTo30() throws Exception {
        String token = registerDoctor("slotdur@example.com", "Dr Slot", cardiologyId);

        // Newly registered doctor defaults to 30-minute slots.
        mockMvc.perform(get("/api/doctors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotDurationMinutes").value(30));

        String update = """
                {"name":"Dr Slot","specialtyId":%d,"slotDurationMinutes":45}
                """.formatted(cardiologyId);
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotDurationMinutes").value(45));
    }

    @Test
    void slotDurationOutOfRangeIsBadRequest() throws Exception {
        String token = registerDoctor("slotbad@example.com", "Dr Slot", cardiologyId);
        String update = """
                {"name":"Dr Slot","specialtyId":%d,"slotDurationMinutes":1}
                """.formatted(cardiologyId);
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.slotDurationMinutes").isNotEmpty());
    }

    @Test
    void combinedSpecialtyAndNameFilterReturnsIntersection() throws Exception {
        registerDoctor("heart1@example.com", "Alice Heart", cardiologyId);
        registerDoctor("heart2@example.com", "Bob Heart", cardiologyId);
        registerDoctor("skin1@example.com", "Alice Skin", dermatologyId);

        // specialtyId (cardiology) AND q ("alice") must both match -> only "Alice Heart".
        mockMvc.perform(get("/api/doctors")
                        .param("specialtyId", cardiologyId.toString())
                        .param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice Heart"));
    }

    @Test
    void doctorUpdateWithBlankNameIsBadRequest() throws Exception {
        String token = registerDoctor("blank@example.com", "Dr Named", cardiologyId);

        // Authenticated DOCTOR so we get past @PreAuthorize and actually hit bean validation.
        mockMvc.perform(put("/api/doctors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void listRespectsPageSize() throws Exception {
        registerDoctor("p1@example.com", "Dr Alpha", cardiologyId);
        registerDoctor("p2@example.com", "Dr Beta", dermatologyId);

        mockMvc.perform(get("/api/doctors").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private String registerDoctor(String email, String name, Long specialtyId) throws Exception {
        String body = """
                {"email":"%s","password":"password123","name":"%s","specialtyId":%d,
                 "cabinetAddress":"Clinic","workingHours":"9-17","phone":"+212"}
                """.formatted(email, name, specialtyId);
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
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
